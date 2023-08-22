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

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.hillert.image.metadata.service.support.ImageLoaderType;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Responsible for persisting and loading the actual image file data.
 *
 * @author Gunnar Hillert
 */
public interface ImageService {

	void init();

	void store(MultipartFile file);

	Stream<Path> loadAll();

	Path load(String filename);

	Resource loadAsResource(String filename);

	/**
	 * Load an image using the provided filename and return a byte array.
	 * @param filename must not be null
	 * @return the image data as byte array
	 * @throws com.hillert.image.metadata.service.support.StorageException in cases the
	 * image could not be read
	 */
	byte[] loadAsBytes(String filename);

	BufferedImage loadAsBufferedImage(Resource imageResource, ImageLoaderType imageLoaderType, String mimeType);

	void deleteAll();

	void delete(String filename);

	void store(byte[] imageBytes, String originalFilename);

	byte[] resizeImage(BufferedImage bufferedImage, String label, Integer targetWidth, String mimeType);

}
