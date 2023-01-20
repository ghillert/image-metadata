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

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.io.Resource;

public class ImageLoader extends Component {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImageLoader.class);

	public BufferedImage loadImageUsingImageIO(Resource imageResource) {
		ImageIO.setUseCache(false);
		BufferedImage img = null;
		try {
			img = ImageIO.read(imageResource.getInputStream());
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
		return img;
	}

	public BufferedImage loadImageUsingAwtToolkit(Resource imageResource) {

		/* Get the toolkit from this Component */
		Toolkit t = Toolkit.getDefaultToolkit();
		/* Begin a retrieval of a remote image */
		Image   i = null;
		try {
			i = t.getImage(imageResource.getURL());
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		/* Create a new MediaTracker linked to this Component */
		MediaTracker m = new MediaTracker(this);
		/* Add the loading image to the MediaTracker,
		with an ID of 1 */
		m.addImage(i, 1);
		/* Explicitly wait for the image to load */
		try {
			m.waitForAll();
		}
		/* Catch the exception */
		catch (InterruptedException ex) {
			LOGGER.warn("Loading of the image was interrupted");
		}

		/* Check the status */
//		if( m.status() & MediaTracker.LOADING )
//			System.out.println("Still Loading - oops, we should never be here!");
//		if( m.status() & MediaTracker.ABORTED )
//			System.out.println("Loading of image aborted");
//		if( m.status() & MediaTracker.ERRORED )
//			System.out.println("Image was errored");
//		if( m.status() & MediaTracker.COMPLETE )
//			System.out.println("Image load complete!");

		return toBufferedImage(i);
	}

	public BufferedImage toBufferedImage(Image image) {
		if (image instanceof BufferedImage) {
			return (BufferedImage) image;
		}

		BufferedImage bi = new BufferedImage(image.getWidth(null), image.getHeight(null),
				BufferedImage.TYPE_INT_RGB);
		Graphics g = bi.getGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();

		return bi;
	}
}
