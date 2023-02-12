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
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import com.hillert.image.metadata.controller.IndexController;
import com.hillert.image.metadata.service.support.ImageLoader;
import com.hillert.image.metadata.service.support.ImageLoaderType;
import com.hillert.image.metadata.service.support.StorageException;
import ij.ImagePlus;
import ij.process.ImageProcessor;
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
import org.springframework.web.multipart.MultipartFile;

/**
 * Default implementation of the {@link ImageService}.
 * @author Gunnar Hillert
 */
@Service
public class DefaultImageService implements ImageService {

	private static final Logger LOGGER = LoggerFactory.getLogger(IndexController.class);

	private final Path rootLocation;

	@Autowired
	public DefaultImageService(StorageProperties properties) {
		this.rootLocation = Paths.get(properties.getLocation());
	}

	@Override
	public void store(byte[] imageBytes, String originalFilename) {
		try (InputStream inputStream = new ByteArrayInputStream(imageBytes)) {
			Path destinationFile = this.rootLocation.resolve(
							Paths.get(originalFilename))
					.normalize().toAbsolutePath();
			Files.copy(inputStream, destinationFile,
					StandardCopyOption.REPLACE_EXISTING);
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void store(MultipartFile file) {
		try {
			if (file.isEmpty()) {
				throw new StorageException("Failed to store empty file.");
			}
			Path destinationFile = this.rootLocation.resolve(
							Paths.get(file.getOriginalFilename()))
					.normalize().toAbsolutePath();
			if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
				// This is a security check
				throw new StorageException(
						"Cannot store file outside current directory.");
			}
			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, destinationFile,
						StandardCopyOption.REPLACE_EXISTING);
			}
		}
		catch (IOException ex) {
			throw new StorageException("Failed to store file.", ex);
		}
	}

	@Override
	public byte[] loadAsBytes(String filename) {
		Resource image = this.loadAsResource(filename);
		byte[] imageBytes = new byte[0];
		try {
			imageBytes = image.getInputStream().readAllBytes();
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		return imageBytes;
	}

	@Override
	public BufferedImage loadAsBufferedImage(Resource imageResource, ImageLoaderType imageLoaderType) {
		if (imageLoaderType == null) {
			imageLoaderType = ImageLoaderType.AWT_TOOLKIT;
		}
		final StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		final ImageLoader imageLoader = new ImageLoader();
		final BufferedImage bufferedImage;
		switch (imageLoaderType) {
			case IMAGE_IO -> {
				bufferedImage = imageLoader.loadImageUsingImageIO(imageResource);
			}
			case AWT_TOOLKIT -> {
				bufferedImage = imageLoader.loadImageUsingAwtToolkit(imageResource);
			}
			default -> throw new IllegalStateException("Unsupported imageLoaderType " + imageLoaderType.name());
		}
		stopWatch.stop();
		LOGGER.info("BufferedImage loaded in {}ms using {}", stopWatch.getTotalTimeMillis(), imageLoaderType.name());
		return bufferedImage;
	}

	@Override
	public Resource loadAsResource(String filename) {
		try {
			Path file = load(filename);
			Resource resource = new UrlResource(file.toUri());
			if (resource.exists() || resource.isReadable()) {
				return resource;
			}
			else {
				throw new StorageException(
						"Could not read file: " + filename);

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
		Resource image = loadAsResource(filename);
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
	public byte[] resizeImage(BufferedImage bufferedImage, String label, Integer targetWidth, ImageLoaderType imageLoaderType) throws IOException {
		if (imageLoaderType == null) {
			imageLoaderType = ImageLoaderType.AWT_TOOLKIT;
		}
		System.setProperty("java.awt.headless", "true");
		Graphics2D g2 = (Graphics2D) bufferedImage.getGraphics();
		RenderingHints rh = new RenderingHints(
				RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		rh.put(RenderingHints.KEY_FRACTIONALMETRICS,
				RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		rh.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
		g2.setRenderingHints(rh);

		ImagePlus image = new ImagePlus("Plant", bufferedImage);

		if (targetWidth == null) {
			targetWidth = image.getWidth(); //TODO
		}
		final int currentWidth = image.getWidth();
		final int currentHeight = image.getHeight();
		final int targetHeight = (int) (((double) targetWidth / currentWidth) * currentHeight);

		LOGGER.info("Current Width: {} - Target Width: {}.", currentWidth, targetWidth);

//		Resource fontResource = new ClassPathResource("/fonts/Montserrat-SemiBold.ttf");
//		InputStream stream = null;
//
//		try {
//			stream = new BufferedInputStream(fontResource.getInputStream());
//		}
//		catch (IOException ex) {
//			ex.printStackTrace();
//		}

		final String fontName = "Montserrat-SemiBold.ttf";
		File tempDirFile = Files.createTempDirectory("metadata").toFile();
		String tempDir = tempDirFile.getAbsolutePath();
		Resource fontResource = new ClassPathResource("/fonts/" + fontName);
		File destinationFontFile = new File(tempDir, fontName);
		Font font;

		try {
			final InputStream stream = new BufferedInputStream(fontResource.getInputStream());
			FileUtils.copyInputStreamToFile(stream, destinationFontFile);
			LOGGER.info("Copied font '{}' to '{}'.", fontName, destinationFontFile.getAbsolutePath());
			font = Font.createFont(Font.TRUETYPE_FONT, destinationFontFile);
			font = font.deriveFont(20f);
		}
		catch (FontFormatException ex) {
			ex.printStackTrace();
			throw new IllegalStateException(ex);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Error creating font.", ex);
		}

//		try {
//			//System.out.println(fontResource.getFilename() + " Font length: .....>>>>>" + stream.readAllBytes().length);
//			Files.createTempFile("+~JF", ".tmp").toFile();
//			Font montserratSemiBold = Font.createFont(Font.TRUETYPE_FONT, stream);
//			//Font montserratSemiBold = Font.createFont(Font.TRUETYPE_FONT, fontAsFile);
//			font = montserratSemiBold.deriveFont(20f);
//		} catch (FontFormatException ex) {
//			ex.printStackTrace();
//			throw new IllegalStateException(ex);
//		} catch (IOException ex) {
//			ex.printStackTrace();
//			throw new IllegalStateException(ex);
//		}

		ImageProcessor ip = image.getProcessor();

		double sigmaFactor = 0.0198;

		int interpolationMethod = ImageProcessor.NONE;

		ip.blurGaussian(sigmaFactor /  ((double) targetWidth / currentWidth));
		ip.setInterpolationMethod(interpolationMethod);
		ImageProcessor scaledImageProcessor = ip.resize(targetWidth, targetHeight);

		scaledImageProcessor.setAntialiasedText(true);
		scaledImageProcessor.setColor(Color.GREEN);
//		scaledImageProcessor.setFont(font);
		scaledImageProcessor.drawString(label, 20, 40);

		image = new ImagePlus("Plant", scaledImageProcessor);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
		//ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("png").next();
		ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
		jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		jpgWriteParam.setCompressionQuality(1.0f);

		ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream);

		jpgWriter.setOutput(ios);
		jpgWriter.write(null, new IIOImage(image.getBufferedImage(), null, null), jpgWriteParam);
		return outputStream.toByteArray();
	}
}
