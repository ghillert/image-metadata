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
import java.awt.image.BufferedImageOp;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import com.hillert.image.metadata.filter.UnsharpFilter;
import com.twelvemonkeys.image.ResampleOp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.MediaType;

/**
 * Contains common utilities for the project.
 *
 * @author Gunnar Hillert
 */
public final class ImageIoTools {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImageIoTools.class);

	private ImageIoTools() {
		throw new AssertionError("This is a static utility class.");
	}

	public static BufferedImage resizeImage(BufferedImage bufferedImage, Integer targetWidth) {

		if (targetWidth == null) {
			targetWidth = bufferedImage.getWidth();
		}

		final int currentWidth = bufferedImage.getWidth();
		final int currentHeight = bufferedImage.getHeight();
		final int targetHeight = (int) (((double) targetWidth / currentWidth) * currentHeight);

		LOGGER.info("Current Width: {} - Target Width: {}.", currentWidth, targetWidth);

		final BufferedImageOp resampler = new ResampleOp(targetWidth, targetHeight, ResampleOp.FILTER_LANCZOS);
		BufferedImage outputBufferedImage = resampler.filter(bufferedImage, null);

		final UnsharpFilter unsharpFilter = new UnsharpFilter();
		unsharpFilter.setRadius(2.0f);
		unsharpFilter.setAmount(0.5f);
		outputBufferedImage = unsharpFilter.filter(outputBufferedImage, null);

		return outputBufferedImage;
	}

	public static byte[] writeImage(BufferedImage bufferedImage, String mimeType) {
		final ImageWriter imageWriter = ImageIO.getImageWritersByMIMEType(mimeType).next();
		final ImageWriteParam imageWriteParam = imageWriter.getDefaultWriteParam();

		switch (mimeType) {
			case MediaType.IMAGE_JPEG_VALUE -> {
				imageWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				imageWriteParam.setCompressionQuality(0.8f);
			}
		}

		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream)) {
			imageWriter.setOutput(ios);
			imageWriter.write(null, new IIOImage(bufferedImage, null, null), imageWriteParam);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to create ImageOutputStream.", ex);
		}

		return outputStream.toByteArray();

	}

}
