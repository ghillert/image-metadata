package com.hillert.image.metadata.controller;

import java.awt.image.BufferedImage;
import java.util.stream.Stream;

import com.hillert.image.metadata.config.MetadataConfigProperties;
import com.hillert.image.metadata.service.ImageService;
import com.hillert.image.metadata.service.MetadataService;
import com.hillert.image.metadata.service.support.ImageLoaderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageServingTests {

	private final ImageService imageService = mock(ImageService.class);

	private final MetadataConfigProperties properties = new MetadataConfigProperties();

	private final Resource file = new ClassPathResource("test-image.jpg");

	private IndexController controller;

	@BeforeEach
	void setUp() {
		this.properties.setDoNotResize(false);
		this.controller = new IndexController(this.imageService, mock(MetadataService.class), new MultipartProperties(),
				this.properties);
		when(this.imageService.loadAsResource("photo.jpg")).thenReturn(this.file);
	}

	@Test
	void doNotResizeServesStoredImageWithoutInitializingImageProcessing() throws Exception {
		this.properties.setDoNotResize(true);
		ResponseEntity<Resource> response = this.controller.serveFile("photo.jpg", 200, true, null, false);
		assertThat(response.getBody()).isSameAs(this.file);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);
		verify(this.imageService, never()).loadAsBufferedImage(any(), any(), anyString());
	}

	@ParameterizedTest
	@MethodSource("nativeLibraryFailures")
	void nativeLibraryFailureFallsBackAndIsNotRetried(LinkageError failure) throws Exception {
		when(this.imageService.loadAsBufferedImage(this.file, ImageLoaderType.IMAGE_IO, "image/jpeg"))
			.thenThrow(failure);
		ResponseEntity<Resource> preview = this.controller.serveFile("photo.jpg", 200, false, null, false);
		assertThat(preview.getBody()).isSameAs(this.file);
		assertThat(preview.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);

		ResponseEntity<Resource> download = this.controller.serveFile("photo.jpg", 400, true, null, true);
		assertThat(download.getBody()).isSameAs(this.file);
		assertThat(download.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
			.isEqualTo("attachment; filename=\"photo.jpg\"");
		assertThat(download.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);
		verify(this.imageService, times(1)).loadAsBufferedImage(this.file, ImageLoaderType.IMAGE_IO, "image/jpeg");
	}

	@Test
	void resizingRemainsAvailableWhenEnabled() throws Exception {
		BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
		byte[] resized = new byte[] { 1, 2, 3 };
		when(this.imageService.loadAsBufferedImage(this.file, ImageLoaderType.IMAGE_IO, "image/jpeg"))
			.thenReturn(image);
		when(this.imageService.resizeImage(image, "photo.jpg", 200, "image/jpeg")).thenReturn(resized);
		ResponseEntity<Resource> response = this.controller.serveFile("photo.jpg", 200, true, null, false);
		assertThat(response.getBody().getContentAsByteArray()).isEqualTo(resized);
	}

	@Test
	void ordinaryImageErrorsAreNotSilenced() {
		when(this.imageService.loadAsBufferedImage(this.file, ImageLoaderType.IMAGE_IO, "image/jpeg"))
			.thenThrow(new IllegalArgumentException("Invalid image"));
		assertThatThrownBy(() -> this.controller.serveFile("photo.jpg", 200, false, null, false))
			.isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid image");
	}

	static Stream<LinkageError> nativeLibraryFailures() {
		return Stream.of(new UnsatisfiedLinkError("Can't load library: libawt_lwawt.dylib"),
				new NoClassDefFoundError("Could not initialize class javax.imageio.ImageIO"));
	}

}
