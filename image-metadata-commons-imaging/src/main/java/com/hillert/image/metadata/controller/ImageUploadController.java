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

import java.io.IOException;

import com.hillert.image.metadata.controller.form.ImageUploadForm;
import com.hillert.image.metadata.model.DirectoryType;
import com.hillert.image.metadata.service.ImageService;
import com.hillert.image.metadata.service.MetadataService;
import com.hillert.image.metadata.service.support.MetadataExtractor;
import jakarta.validation.Valid;
import org.apache.commons.imaging.ImageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StopWatch;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller that is responsible for handling file uploads.
 *
 * @author Gunnar Hillert
 */
@Controller
public class ImageUploadController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImageUploadController.class);

	private static final String INDEX_TEMPLATE = "index";

	private final ImageService imageService;

	private final MetadataService metadataService;

	public ImageUploadController(ImageService imageService, MetadataService metadataService) {
		this.imageService = imageService;
		this.metadataService = metadataService;
	}

	@GetMapping({ "/upload-error" })
	public String uploadError(Model model) {
		return "uploadError";
	}

	@PostMapping("/")
	public String handleFileUpload(@Valid @ModelAttribute("imageUploadForm") ImageUploadForm imageUploadForm,
			BindingResult result, RedirectAttributes redirectAttributes) {

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		if (result.hasErrors()) {
			return INDEX_TEMPLATE;
		}

		final MultipartFile imageFile = imageUploadForm.getImageFile();

		byte[] imageBytes;
		try {
			imageBytes = imageFile.getBytes();
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to retrieve image data from upload file.", ex);
		}

		final ImageInfo imageInfo = MetadataExtractor.loadImageInfo(imageBytes);
		LOGGER.info("File Content-Type - Submitted as '{}' verified as '{}'.", imageFile.getContentType(),
				imageInfo.getMimeType());

		final String contentType = imageFile.getContentType();
		if (contentType == null) {
			result.reject("upload.mime-type.required", null);
			return INDEX_TEMPLATE;
		}
		else if (!contentType.equalsIgnoreCase(imageInfo.getMimeType())) {
			result.reject("upload.mime-type.mot.match",
					new Object[] { imageFile.getContentType(), imageInfo.getMimeType() }, null);
			return INDEX_TEMPLATE;
		}

		if (Boolean.TRUE.equals(imageUploadForm.getRemoveMetadata())) {
			imageBytes = this.metadataService.purge(imageBytes, DirectoryType.EXIF);
			imageBytes = this.metadataService.purge(imageBytes, DirectoryType.IPTC);
			imageBytes = this.metadataService.purge(imageBytes, DirectoryType.XMP);
		}

		if (Boolean.TRUE.equals(imageUploadForm.isUpdateMetadataNeeded())) {
			imageBytes = this.metadataService.updateMetadata(imageBytes, imageUploadForm.getPopulateWindowsTags(),
					imageUploadForm.getReferenceId(), imageUploadForm.getTitle());
		}

		this.imageService.store(imageBytes, imageFile.getOriginalFilename());

		redirectAttributes.addFlashAttribute("success",
				"You successfully uploaded " + imageFile.getOriginalFilename() + "!");
		stopWatch.stop();
		LOGGER.info("Image uploaded and processed in {}ms.", stopWatch.getTotalTimeMillis());
		return "redirect:/";
	}

}
