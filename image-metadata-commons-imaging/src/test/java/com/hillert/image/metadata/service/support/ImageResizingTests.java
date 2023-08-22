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

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gunnar Hillert
 */
public class ImageResizingTests {

	private final Resource rawXmlResource = new ClassPathResource("/rawXml.xml");

	private final Resource formattedXmlResource = new ClassPathResource("/formattedXml.xml");

	@BeforeAll
	static void beforeAll() {
		System.setProperty("java.awt.headless", Boolean.TRUE.toString());
	}

	@Test
	void testImageResizing() throws IOException {
		final Resource imageResource = new ClassPathResource("/test-image.jpg");
		final ImageLoader imageLoader = new ImageLoader();
		final BufferedImage bufferedImage = imageLoader.loadImageUsingImageIO(imageResource);
		final BufferedImage resizedBufferedImage = ImageIoTools.resizeImage(bufferedImage, 200);
		assertThat(resizedBufferedImage.getWidth()).isEqualTo(200);
	}

	public static void main(String... args) {
		final Resource imageResource = new ClassPathResource("/test-image.jpg");
		final ImageLoader imageLoader = new ImageLoader();
		final BufferedImage bufferedImage = imageLoader.loadImageUsingImageIO(imageResource);
		final BufferedImage resizedBufferedImage = ImageIoTools.resizeImage(bufferedImage, 200);

		final JFrame jFrame = new JFrame();
		final ImageIcon imageIcon = new ImageIcon(resizedBufferedImage);
		final JLabel jLabel = new JLabel(imageIcon);
		jFrame.add(jLabel);
		jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jFrame.pack();
		jFrame.setVisible(true);
	}
}
