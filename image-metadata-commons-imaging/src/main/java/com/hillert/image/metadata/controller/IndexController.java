/*
 * Copyright (c) 2023, 2025 Gunnar Hillert.
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
package com.hillert.image.metadata.controller;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.TimeZone;

import com.hillert.image.metadata.controller.form.ImageUploadForm;
import com.hillert.image.metadata.model.Metadata;
import com.hillert.image.metadata.service.ImageService;
import com.hillert.image.metadata.service.MetadataService;
import com.hillert.image.metadata.service.support.ImageLoaderType;
import com.hillert.image.metadata.service.support.MetadataExtractor;
import org.apache.commons.imaging.ImageInfo;

import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Root controller.
 *
 * @author Gunnar Hillert
 */
@Controller
public class IndexController {

	private static final String INDEX_TEMPLATE = "index";

	private final ImageService imageService;

	private final MetadataService metadataService;

	private final MultipartProperties multipartProperties;

	public IndexController(ImageService imageService, MetadataService metadataService,
			MultipartProperties multipartProperties) {
		this.imageService = imageService;
		this.metadataService = metadataService;
		this.multipartProperties = multipartProperties;
	}

	@GetMapping({ "/" })
	public String index(Model model) {
		model.addAttribute("imageUploadForm", new ImageUploadForm());
		model.addAttribute("uploadSizeLimit", this.multipartProperties.getMaxFileSize().toMegabytes());
		model.addAttribute("files", this.imageService.loadAll().map((path) -> path.getFileName().toString()).toList());
		return INDEX_TEMPLATE;
	}

	@GetMapping({ "/delete-image/{filename:.+}" })
	public String deleteImage(@PathVariable(name = "filename") String filename, RedirectAttributes redirectAttributes) {
		this.imageService.delete(filename);
		redirectAttributes.addFlashAttribute("success", "Image deleted!");
		return "redirect:/";
	}

	@GetMapping({ "/image-details/{filename:.+}" })
	public String getImageDetails(@PathVariable(name = "filename") String filename, Model model, TimeZone timezone) {

		final Resource imageResource = this.imageService.loadAsResource(filename);

		final Metadata metadata = this.metadataService.getExifData(imageResource);
		metadata.setTimeZone(timezone.toZoneId());
		model.addAttribute("filename", filename);

		model.addAttribute("metadata", metadata.getDirectoriesPerType());
		model.addAttribute("metadataCount", metadata.getDirectoryCount());
		model.addAttribute("mapLocation", metadata.getGnssInfo());
		model.addAttribute("xmpData", metadata.getXmpData());

		model.addAttribute("imageDescription", this.metadataService.getImageDescription(imageResource));
		return "imageDetails";
	}

	@GetMapping("/images/{filename:.+}")
	@ResponseBody
	public ResponseEntity<Resource> serveFile(@PathVariable(name = "filename") String filename,
			@RequestParam(name = "width", required = false) Integer width,
			@RequestParam(name = "addLabel", required = false, defaultValue = "true") boolean addLabel,
			@RequestParam(name = "imageLoaderType", required = false) ImageLoaderType imageLoaderType,
			@RequestParam(name = "download", required = false, defaultValue = "false") boolean download)
			throws IOException {
		final Resource file = this.imageService.loadAsResource(filename);
		byte[] imageBytes = null;
		imageLoaderType = ImageLoaderType.IMAGE_IO;
		final ImageInfo imageInfo = MetadataExtractor.loadImageInfo(file.getContentAsByteArray());

		final BufferedImage bufferedImage = this.imageService.loadAsBufferedImage(file, imageLoaderType,
				imageInfo.getMimeType());

		final String label;

		if (addLabel) {
			label = filename;
		}
		else {
			label = null;
		}

		imageBytes = this.imageService.resizeImage(bufferedImage, label, width, imageInfo.getMimeType());

		final ByteArrayResource byteArrayResource = new ByteArrayResource(imageBytes);
		if (download) {
			return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.body(byteArrayResource);
		}
		else {
			return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(imageInfo.getMimeType()))
				.body(byteArrayResource);
		}
	}

}
