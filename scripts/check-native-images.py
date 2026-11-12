#!/usr/bin/env python3
"""Exercise real native resizing/labels; the stored-image fallback must not pass.

Usage: python3 scripts/check-native-images.py path/to/native-executable
Uses only the standard library, a loopback port, and temporary upload storage.
"""

import argparse
import http.client
import pathlib
import socket
import struct
import subprocess
import sys
import tempfile
import time


def jpeg_size(data):
    if data[:2] != b"\xff\xd8":
        raise AssertionError("Response is not a JPEG")
    offset = 2
    while offset < len(data):
        assert data[offset] == 0xFF, "Invalid JPEG marker"
        while data[offset] == 0xFF:
            offset += 1
        marker = data[offset]
        offset += 1
        length = int.from_bytes(data[offset:offset + 2], "big")
        if marker in (0xC0, 0xC1, 0xC2):
            height, width = struct.unpack(">HH", data[offset + 3:offset + 7])
            return width, height
        offset += length
    raise AssertionError("JPEG has no frame header")


def check(binary, output_directory=None):
    root = pathlib.Path(__file__).resolve().parents[1]
    fixture = (root / "image-metadata-commons-imaging/src/test/resources/test-image.jpg").read_bytes()
    original_width, original_height = jpeg_size(fixture)
    with tempfile.TemporaryDirectory(prefix="image-native-rendering-") as temporary:
        temporary = pathlib.Path(temporary)
        with socket.socket() as listener:
            listener.bind(("127.0.0.1", 0))
            port = listener.getsockname()[1]
        with (temporary / "application.log").open("w+") as log:
            process = subprocess.Popen([
                str(binary), "--server.address=127.0.0.1", f"--server.port={port}",
                f"--storage.location={temporary / 'uploads'}",
                "--metadata.do-not-resize=false", "--metadata.ai.create-image-description=false",
                # Satisfy older AI auto-configuration without using a real credential.
                # Image descriptions are disabled above, so no AI requests are made.
                "--spring.ai.openai.api-key=native-smoke-test-key", "--spring.ai.openai.chat.api-key=native-smoke-test-key",
            ], cwd=temporary, stdout=log, stderr=subprocess.STDOUT)

            def request(path, method="GET", body=None, headers=None):
                connection = http.client.HTTPConnection("127.0.0.1", port, timeout=30)
                try:
                    connection.request(method, path, body=body, headers=headers or {})
                    response = connection.getresponse()
                    return response.status, response.read()
                finally:
                    connection.close()

            try:
                for _ in range(120):
                    if process.poll() is not None:
                        raise AssertionError("Native application exited during startup")
                    try:
                        assert request("/")[0] == 200
                        break
                    except ConnectionRefusedError:
                        time.sleep(0.25)
                else:
                    raise AssertionError("Native application did not start")

                boundary = "nativeImageRenderingCheck"
                body = (f'--{boundary}\r\nContent-Disposition: form-data; name="imageFile"; '
                        'filename="test-image.jpg"\r\nContent-Type: image/jpeg\r\n\r\n').encode()
                body += fixture + f"\r\n--{boundary}--\r\n".encode()
                assert request("/", "POST", body, {
                    "Content-Type": f"multipart/form-data; boundary={boundary}"
                })[0] == 302, "Upload failed"

                for width in (200, 800):
                    images = []
                    for label in ("false", "true"):
                        status, image = request(f"/images/test-image.jpg?width={width}&addLabel={label}")
                        assert status == 200, f"Rendering returned HTTP {status}"
                        expected = (width, int(width / original_width * original_height))
                        assert jpeg_size(image) == expected, "Resize failed (possibly served fallback)"
                        images.append(image)
                        if output_directory is not None:
                            output_directory.mkdir(parents=True, exist_ok=True)
                            (output_directory / f"{width}-label-{label}.jpg").write_bytes(image)
                    assert images[0] != images[1], "Label did not change the rendered image"
                    print(f"PASS: {width}px native resize and filename label", flush=True)
                log.flush()
                log.seek(0)
                output = log.read()
                assert "UnsatisfiedLinkError" not in output, "Native library loading failed"
                assert "NoClassDefFoundError" not in output, "Native image processing failed"
            except BaseException:
                log.flush()
                log.seek(0)
                print(log.read(), file=sys.stderr)
                raise
            finally:
                process.terminate()
                try:
                    process.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("binary", type=pathlib.Path)
    parser.add_argument("--output-directory", type=pathlib.Path, help="Keep rendered images for visual inspection")
    args = parser.parse_args()
    check(args.binary.resolve(strict=True), args.output_directory)
