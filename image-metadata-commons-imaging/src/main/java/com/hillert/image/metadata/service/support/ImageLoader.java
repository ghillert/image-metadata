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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Provides methods to load images using Java. For demonstration purposes, 2 approaches
 * are provided:
 * <ul>
 * <li>Load images using ImageIO
 * <li>Load images using AwtToolkit
 * </ul>
 *
 * @author Gunnar Hillert
 */
public class ImageLoader extends Component {

	private static final long serialVersionUID = 1;

	private static final Logger LOGGER = LoggerFactory.getLogger(ImageLoader.class);

	public BufferedImage loadImageUsingImageIO(Resource imageResource) {
		Assert.notNull(imageResource, "imageResource must not be null.");
		LOGGER.debug("Using ImageIO to load image: '{}'.", imageResource);

		ImageIO.setUseCache(false);
		final BufferedImage bufferedImage;
		try (InputStream inputStream = imageResource.getInputStream()) {
			bufferedImage = ImageIO.read(inputStream);
		}
		catch (IOException ex) {
			throw new StorageException("Unable to read image.", ex);
		}
		return bufferedImage;
	}

	/**
	 * This method will load an image using the rather old AwtToolkit. The benefit is that
	 * it is noticeably fast than loading an image using ImageIO.
	 *
	 * See:
	 * https://stackoverflow.com/questions/15121863/image-made-through-toolkit-returns-1-as-width-height
	 * @param imageResource the image data. Must not be null.
	 * @return the loaded BufferedImage
	 * @throws StorageException in case the image could not be loaded
	 */
	public BufferedImage loadImageUsingAwtToolkit(Resource imageResource) {
		Assert.notNull(imageResource, "imageResource must not be null.");

		final URL imageUrl;

		try {
			imageUrl = imageResource.getURL();
		}
		catch (IOException ex) {
			throw new IllegalStateException("Cannot resolve URL " + imageResource.getFilename(), ex);
		}

		// LOGGER.info("Using AwtToolkit to load image.");

		/* Get the toolkit from this Component */
		final Toolkit abstractWindowToolkit = Toolkit.getDefaultToolkit();
		/* Begin a retrieval of a remote image */

		/* Image data is cached */
		final Image image = abstractWindowToolkit.getImage(imageUrl);

		/* This method will not employ caching */
		// Image image = abstractWindowToolkit.createImage(imageUrl);

		// call to clear data from cache
		// image.flush();

		/* Create a new MediaTracker linked to this Component */
		final MediaTracker mediaTracker = new MediaTracker(this);

		/*
		 * Add the loading image to the MediaTracker, with an ID of 1
		 */
		mediaTracker.addImage(image, 1);

		/* Wait for the image to load */
		try {
			mediaTracker.waitForAll();
		}

		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new StorageException(ex.getMessage(), ex);
		}

		if (mediaTracker.isErrorAny()) {
			throw new StorageException("There was an error loading the image.");
		}

		return toBufferedImage(image);
	}

	public BufferedImage toBufferedImage(Image image) {
		if (image instanceof BufferedImage bufferedImage) {
			return bufferedImage;
		}

		final BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null),
				// BufferedImage.TYPE_INT_RGB);
				BufferedImage.TYPE_INT_ARGB); // Needed for transparent PNGs
		final Graphics g = bufferedImage.getGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();

		return bufferedImage;
	}

}
