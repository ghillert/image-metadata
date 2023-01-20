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
package com.hillert.image.metadata.controller;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.TimeZone;
import java.util.stream.Collectors;

import com.hillert.image.metadata.controller.form.ImageUploadForm;
import com.hillert.image.metadata.model.DirectoryType;
import com.hillert.image.metadata.model.Metadata;
import com.hillert.image.metadata.service.ImageService;
import com.hillert.image.metadata.service.MetadataService;
import com.hillert.image.metadata.service.support.ImageLoaderType;
import jakarta.validation.Valid;

import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Root controller.
 * @author Gunnar Hillert
 */
@Controller
public class IndexController {

	private final ImageService imageService;

	private final MetadataService metadataService;

	private final MultipartProperties multipartProperties;

	public IndexController(ImageService imageService, MetadataService metadataService, MultipartProperties multipartProperties) {
		this.imageService = imageService;
		this.metadataService = metadataService;
		this.multipartProperties = multipartProperties;
	}

	@GetMapping({"/"})
	public String index(Model model) {

		model.addAttribute("imageUploadForm", new ImageUploadForm());
		model.addAttribute("uploadSizeLimit", this.multipartProperties.getMaxFileSize().toMegabytes());
		model.addAttribute("files", this.imageService.loadAll()
				.map((path) -> path.getFileName().toString())
				.collect(Collectors.toList()));
		return "index";
	}

	@GetMapping({"/uploadError"})
	public String uploadError(Model model) {
		return "uploadError";
	}

	@GetMapping({"/delete-image/{filename:.+}"})
	public String deleteImage(@PathVariable String filename, RedirectAttributes redirectAttributes) {
		this.imageService.delete(filename);
		redirectAttributes.addFlashAttribute("message",
				"Image deleted!");
		return "redirect:/";
	}

	@GetMapping({"/image-details/{filename:.+}"})
	public String getImageDetails(@PathVariable String filename, Model model, TimeZone timezone) {

		Metadata metadata = this.metadataService.getExifData(this.imageService.loadAsResource(filename));
		metadata.setTimeZone(timezone.toZoneId());
		model.addAttribute("filename", filename);

		model.addAttribute("metadata", metadata.getDirectoriesPerType());
		model.addAttribute("metadataCount", metadata.getDirectoryCount());
		model.addAttribute("mapLocation", metadata.getGnssInfo());
		model.addAttribute("xmpData", metadata.getXmpData());
		return "imageDetails";
	}

	@PostMapping("/")
	public String handleFileUpload(@Valid @ModelAttribute("imageUploadForm") ImageUploadForm imageUploadForm,
			BindingResult result, RedirectAttributes redirectAttributes) {

		if (result.hasErrors()) {
			return "index";
		}

		MultipartFile file = imageUploadForm.getImageFile();
		this.metadataService.getExifData(file.getResource());
		this.imageService.store(file);

		if (Boolean.TRUE.equals(imageUploadForm.getRemoveMetadata())) {
			byte[] imageBytes = this.imageService.loadAsBytes(file.getOriginalFilename());
			imageBytes = this.metadataService.purge(imageBytes, DirectoryType.EXIF);
			imageBytes = this.metadataService.purge(imageBytes, DirectoryType.IPTC);
			imageBytes = this.metadataService.purge(imageBytes, DirectoryType.XMP);
			this.imageService.store(imageBytes, file.getOriginalFilename());
		}

		redirectAttributes.addFlashAttribute("message",
				"You successfully uploaded " + file.getOriginalFilename() + "!");
		return "redirect:/";
	}

	@GetMapping("/images/{filename:.+}")
	@ResponseBody
	public ResponseEntity<Resource> serveFile(
			@PathVariable String filename,
			@RequestParam(required = false) Integer width,
			@RequestParam(required = false) ImageLoaderType imageLoaderType,
			@RequestParam(required = false, defaultValue = "false") boolean download) throws IOException {
		final Resource file = this.imageService.loadAsResource(filename);
		final BufferedImage bufferedImage = this.imageService.loadAsBufferedImage(file, imageLoaderType);
		final byte[] imageData = this.imageService.resizeImage(bufferedImage, filename, width, imageLoaderType);
		final ByteArrayResource byteArrayResource = new ByteArrayResource(imageData);
		if (download) {
			return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
				"attachment; filename=\"" + file.getFilename() + "\"").body(file);
		}
		else {
			return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(byteArrayResource); //TODO Right media type
		}
	}
}

