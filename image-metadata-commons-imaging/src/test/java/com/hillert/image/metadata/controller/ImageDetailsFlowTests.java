package com.hillert.image.metadata.controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import com.hillert.image.metadata.model.Directory;
import com.hillert.image.metadata.model.DirectoryType;
import com.hillert.image.metadata.model.Metadata;
import com.hillert.image.metadata.service.ImageService;
import com.hillert.image.metadata.service.MetadataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "spring.ai.openai.api-key=test-api-key")
class ImageDetailsFlowTests {

	@LocalServerPort
	private int port;

	@MockitoBean
	private ImageService imageService;

	@MockitoBean
	private MetadataService metadataService;

	private final Resource image = new ByteArrayResource(new byte[0]);

	@BeforeEach
	void prepareImage() {
		Metadata metadata = new Metadata();
		metadata.addDirectories(List.of(new Directory(DirectoryType.EXIF, "Camera", "Test camera")));
		when(this.imageService.loadAsResource("photo.jpg")).thenReturn(this.image);
		when(this.metadataService.getExifData(this.image)).thenReturn(metadata);
	}

	@Test
	void descriptionFailureDoesNotHideImageMetadata() throws Exception {
		when(this.metadataService.getImageDescription(this.image))
			.thenThrow(new IllegalStateException("AI service unavailable: sensitive upstream error"));
		HttpResponse<String> response = details();
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("Test camera", "description is temporarily unavailable")
			.doesNotContain("sensitive upstream error");
	}

	@Test
	void successfulDescriptionIsRendered() throws Exception {
		when(this.metadataService.getImageDescription(this.image)).thenReturn("A sunny landscape.");
		HttpResponse<String> response = details();
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("Test camera", "A sunny landscape.")
			.doesNotContain("description is temporarily unavailable");
	}

	private HttpResponse<String> details() throws Exception {
		URI uri = URI.create("http://localhost:" + this.port + "/image-details/photo.jpg");
		return HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri).GET().build(),
				HttpResponse.BodyHandlers.ofString());
	}

}
