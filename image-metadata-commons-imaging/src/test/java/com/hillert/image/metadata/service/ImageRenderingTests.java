package com.hillert.image.metadata.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.hillert.image.metadata.config.StorageConfigProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Check rendered pixels as well as dimensions, so a no-op label cannot pass.
 *
 * @author Gunnar Hillert
 */
class ImageRenderingTests {

	@Test
	void rendersFilenameOnResizedImage() throws IOException {
		final BufferedImage input = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
		final Graphics2D graphics = input.createGraphics();
		try {
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, input.getWidth(), input.getHeight());
		}
		finally {
			graphics.dispose();
		}
		final DefaultImageService service = new DefaultImageService(new StorageConfigProperties());
		final BufferedImage plain = ImageIO.read(new ByteArrayInputStream(
				service.resizeImage(input, null, 200, "image/png")));
		final BufferedImage labeled = ImageIO.read(new ByteArrayInputStream(
				service.resizeImage(input, "test-image.jpg", 200, "image/png")));
		assertThat(labeled.getWidth()).isEqualTo(200);
		assertThat(labeled.getHeight()).isEqualTo(150);
		assertThat(redPixels(plain)).isZero();
		assertThat(redPixels(labeled)).isGreaterThan(50);
	}

	private int redPixels(BufferedImage image) {
		int count = 0;
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				final Color pixel = new Color(image.getRGB(x, y));
				if (pixel.getRed() > 150 && pixel.getGreen() < 100 && pixel.getBlue() < 100) {
					count++;
				}
			}
		}
		return count;
	}

}
