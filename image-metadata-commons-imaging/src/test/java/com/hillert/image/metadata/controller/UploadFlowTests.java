package com.hillert.image.metadata.controller;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "spring.ai.openai.api-key=", "spring.ai.openai.chat.api-key=",
				"metadata.ai.create-image-description=true" })
class UploadFlowTests {

	@TempDir
	static Path storage;

	@LocalServerPort
	private int port;

	@DynamicPropertySource
	static void storageProperties(DynamicPropertyRegistry registry) {
		registry.add("storage.location", () -> storage.toString());
	}

	@Test
	void firstUploadRedirectsToWorkspaceAndRetainsFlashMessage() throws Exception {
		CookieManager cookies = new CookieManager();
		HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).build();
		URI workspace = URI.create("http://localhost:" + this.port + "/");
		String boundary = "ImageMetadataUploadBoundary";
		byte[] prefix = ("--" + boundary + "\r\n"
				+ "Content-Disposition: form-data; name=\"imageFile\"; filename=\"upload-test.jpg\"\r\n"
				+ "Content-Type: image/jpeg\r\n\r\n").getBytes(StandardCharsets.UTF_8);
		byte[] image = new ClassPathResource("test-image.jpg").getContentAsByteArray();
		byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
		HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.concat(HttpRequest.BodyPublishers.ofByteArray(prefix),
				HttpRequest.BodyPublishers.ofByteArray(image), HttpRequest.BodyPublishers.ofByteArray(suffix));
		HttpRequest upload = HttpRequest.newBuilder(workspace)
			.header("Content-Type", "multipart/form-data; boundary=" + boundary)
			.POST(body)
			.build();

		assertThat(cookies.getCookieStore().getCookies()).isEmpty();
		HttpResponse<String> response = client.send(upload, HttpResponse.BodyHandlers.ofString());
		assertThat(response.statusCode()).isEqualTo(302);
		String location = response.headers().firstValue("Location").orElseThrow();
		assertThat(location).doesNotContain(";jsessionid=");
		assertThat(workspace.resolve(location)).isEqualTo(workspace);
		assertThat(cookies.getCookieStore().getCookies()).isNotEmpty();

		HttpResponse<String> redirected = client.send(HttpRequest.newBuilder(workspace.resolve(location)).GET().build(),
				HttpResponse.BodyHandlers.ofString());
		assertThat(redirected.statusCode()).isEqualTo(200);
		assertThat(redirected.body()).contains("You successfully uploaded upload-test.jpg!", "Inspect metadata");

		HttpResponse<String> details = client.send(
				HttpRequest.newBuilder(workspace.resolve("/image-details/upload-test.jpg")).GET().build(),
				HttpResponse.BodyHandlers.ofString());
		assertThat(details.statusCode()).isEqualTo(200);
		assertThat(details.body()).contains("Image descriptions are disabled.", "Metadata", "upload-test.jpg")
			.doesNotContain("description is temporarily unavailable");
	}

}
