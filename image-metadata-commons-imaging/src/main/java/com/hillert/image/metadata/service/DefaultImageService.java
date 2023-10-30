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
package com.hillert.image.metadata.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import com.hillert.image.metadata.config.StorageConfigProperties;
import com.hillert.image.metadata.service.support.CommonUtils;
import com.hillert.image.metadata.service.support.ImageIoTools;
import com.hillert.image.metadata.service.support.ImageLoader;
import com.hillert.image.metadata.service.support.ImageLoaderType;
import com.hillert.image.metadata.service.support.StorageException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Default implementation of the {@link ImageService}.
 *
 * @author Gunnar Hillert
 */
@Service
public class DefaultImageService implements ImageService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultImageService.class);

	private final Path rootLocation;

	@Autowired
	public DefaultImageService(StorageConfigProperties properties) {
		this.rootLocation = Paths.get(properties.getLocation());
	}

	@Override
	public void store(byte[] imageBytes, String originalFilename) {
		try (InputStream inputStream = new ByteArrayInputStream(imageBytes)) {
			Path destinationFile = this.rootLocation.resolve(Paths.get(originalFilename)).normalize().toAbsolutePath();
			Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
		}
		catch (IOException ex) {
			throw new StorageException("Unable to store file " + originalFilename, ex);
		}
	}

	@Override
	public void store(MultipartFile file) {
		try {
			if (file.isEmpty()) {
				throw new StorageException("Failed to store empty file.");
			}
			final Path destinationFile = this.rootLocation.resolve(Paths.get(file.getOriginalFilename()))
				.normalize()
				.toAbsolutePath();
			final Path parentPath = destinationFile.getParent();

			if (parentPath == null) {
				throw new StorageException("Parent path cannot be null.");
			}
			if (!parentPath.equals(this.rootLocation.toAbsolutePath())) {
				throw new StorageException("Cannot store file outside current directory.");
			}
			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		catch (IOException ex) {
			throw new StorageException("Failed to store file.", ex);
		}
	}

	@Override
	public byte[] loadAsBytes(String filename) {
		final Resource imageResource = this.loadAsResource(filename);
		return CommonUtils.resourceToBytes(imageResource);
	}

	@Override
	public BufferedImage loadAsBufferedImage(Resource imageResource, ImageLoaderType imageLoaderType, String mimeType) {
		if (imageLoaderType == null) {
			imageLoaderType = ImageLoaderType.AWT_TOOLKIT;
		}
		final StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		final ImageLoader imageLoader = new ImageLoader();
		final BufferedImage bufferedImage;
		final String imageLoaderTypeName = imageLoaderType.name();
		switch (imageLoaderType) {
			case IMAGE_IO -> {
				bufferedImage = imageLoader.loadImageUsingImageIO(imageResource);
			}
			case AWT_TOOLKIT -> {
				bufferedImage = imageLoader.loadImageUsingAwtToolkit(imageResource);
			}
			default -> throw new IllegalStateException("Unsupported imageLoaderType " + imageLoaderTypeName);
		}
		stopWatch.stop();
		LOGGER.info("BufferedImage loaded in {}ms using {}", stopWatch.getTotalTimeMillis(), imageLoaderTypeName);
		return bufferedImage;
	}

	@Override
	public Resource loadAsResource(String filename) {
		try {
			final Path file = load(filename);
			final Resource resource = new UrlResource(file.toUri());
			if (resource.exists() || resource.isReadable()) {
				return resource;
			}
			else {
				throw new StorageException("Could not read file: " + filename);
			}
		}
		catch (MalformedURLException ex) {
			throw new StorageException("Could not read file: " + filename, ex);
		}
	}

	@Override
	public Path load(String filename) {
		return this.rootLocation.resolve(filename);
	}

	@Override
	public void init() {
		try {
			Files.createDirectories(this.rootLocation);
		}
		catch (IOException ex) {
			throw new StorageException("Could not initialize storage", ex);
		}
	}

	@Override
	public void deleteAll() {
		FileSystemUtils.deleteRecursively(this.rootLocation.toFile());
	}

	@Override
	public void delete(String filename) {
		final Resource image = loadAsResource(filename);
		if (image.exists()) {
			try {
				Files.delete(image.getFile().toPath());
			}
			catch (IOException ex) {
				throw new StorageException("Could not delete file.", ex);
			}
		}
	}

	@Override
	public Stream<Path> loadAll() {
		try {
			return Files.walk(this.rootLocation, 1)
				.filter((path) -> !path.equals(this.rootLocation))
				.map(this.rootLocation::relativize);
		}
		catch (IOException ex) {
			throw new StorageException("Failed to read stored files", ex);
		}

	}

	@Override
	public byte[] resizeImage(BufferedImage bufferedImage, String label, Integer targetWidth, String mimeType) {

		final BufferedImage outputBufferedImage = ImageIoTools.resizeImage(bufferedImage, targetWidth);

		if (!StringUtils.hasText(label)) {
			return ImageIoTools.writeImage(outputBufferedImage, mimeType);
		}

		final String fontName = "Montserrat-SemiBold.ttf";
		final File tempDirFile;
		try {
			tempDirFile = Files.createTempDirectory("metadata").toFile();
		}
		catch (IOException ex) {
			throw new IllegalStateException("Cannot create temp directory.", ex);
		}
		final String tempDir = tempDirFile.getAbsolutePath();
		final Resource fontResource = new ClassPathResource("/fonts/" + fontName);
		final File destinationFontFile = new File(tempDir, fontName);
		final Font font;
		final float fontSize = 20f;
		try (InputStream stream = new BufferedInputStream(fontResource.getInputStream())) {
			FileUtils.copyInputStreamToFile(stream, destinationFontFile);
			LOGGER.info("Copied font '{}' to '{}'.", fontName, destinationFontFile.getAbsolutePath());
			font = Font.createFont(Font.TRUETYPE_FONT, destinationFontFile).deriveFont(fontSize);
		}
		catch (FontFormatException ex) {
			throw new IllegalStateException(
					"Font file does not contain the required font tables for the specified format.", ex);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Error creating font.", ex);
		}

		if (outputBufferedImage.getGraphics() instanceof Graphics2D graphics2D) {
			setGraphicsQuality(graphics2D);
			render(graphics2D, label, 2, 2 + (int) Math.ceil(fontSize),
					font, outputBufferedImage.getWidth(), outputBufferedImage.getHeight());
		}
		else {
			throw new IllegalStateException(
					"outputBufferedImage.getGraphics() is not an instance of Graphics2D but was "
							+ outputBufferedImage.getGraphics().getClass().getSimpleName());
		}

		return ImageIoTools.writeImage(outputBufferedImage, mimeType);
	}

	public static void setGraphicsQuality(Graphics2D g2D) {
		final RenderingHints rh = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		rh.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		rh.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

		rh.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		rh.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		g2D.setRenderingHints(rh);
	}

	private void render(Graphics g, String stringToRender, int x, int y, Font font, int width, int height) {
		g.setFont(font);
		g.setColor(Color.green);
//		g.drawString(stringToRender, x, y);

		final JLabel label = new JLabel();
		label.setForeground(Color.red);
		label.setSize(width - (2 * x), height - (2 * y));
		label.setFont(font);
		label.setVerticalAlignment(SwingConstants.TOP);
		label.setText("<html><p style=\"width:" + width + "px\">" + stringToRender + "</p></html>");
		g.translate(x, y);
		label.paint(g);
	}

}
