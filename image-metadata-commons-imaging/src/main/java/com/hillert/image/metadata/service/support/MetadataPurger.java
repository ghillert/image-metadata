/*
 * Copyright (c) 2023 Gunnar Hillert.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hillert.image.metadata.service.support;

import java.io.File;

/**
 * Responsible for removing image metadata from imaga data.
 * @author Gunnar Hillert
 */
public class MetadataPurger {

	void remove(File file) {
//		new ExifRewriter()
//		ByteSource byteSource = new ByteSourceFile(file);
//		final List<JFIFPiece> pieces = new ArrayList<>();
//		JpegUtils.Visitor visitor = new JpegUtils.Visitor() {
//			@Override
//			public boolean visitSegment(int marker, byte[] markerBytes, int segmentLength, byte[] segmentLengthBytes,
//			                            byte[] segmentData) throws ImageReadException, IOException {
//				// keep only the APP0 marker
//				if ("ffe0".equals(Integer.toHexString(marker))) {
//					final JFIFPiece piece = new JFIFPieceSegment(marker, markerBytes, segmentLengthBytes, segmentData);
//					pieces.add(piece);
//				}
//				return true;
//			}
//
//			@Override
//			public void visitSOS(int marker, byte[] markerBytes, byte[] imageData) {
//				pieces.add(new JFIFPieceImageData(markerBytes, imageData));
//			}
//
//			@Override
//			public boolean beginSOS() {
//				return true;
//			}
//		};
//		new JpegUtils().traverseJFIF(byteSource, visitor);
//		OutputStream newImage = new FileOutputStream(new File("/tmp/02.jpg"));
//		try (DataOutputStream os = new DataOutputStream(newImage)) {
//			JpegConstantsz.SOI.writeTo(os);
//			for (final JFIFPiece piece : pieces) {
//				piece.write(os);
//			}
//		}
	}
}
