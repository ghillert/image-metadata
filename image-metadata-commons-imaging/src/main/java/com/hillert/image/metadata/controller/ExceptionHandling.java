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

import com.hillert.image.metadata.service.support.ImageProcessingException;
import com.hillert.image.metadata.service.support.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Exception Handler.
 *
 * @author Gunnar Hillert
 */
@ControllerAdvice
public class ExceptionHandling {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandling.class);

	private final MultipartProperties multipartProperties;

	public ExceptionHandling(MultipartProperties multipartProperties) {
		this.multipartProperties = multipartProperties;
	}

	@ExceptionHandler(StorageException.class)
	public String storageExceptionErrorHandler(StorageException ex, RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("error", ex.getMessage());
		return "redirect:/";
	}

	@ExceptionHandler(ImageProcessingException.class)
	public String multipartErrorHandler(ImageProcessingException ex, RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("error", ex.getMessage());
		LOGGER.error("ImageProcessingException caught!", ex);
		return "redirect:/";
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public String multipartErrorHandler(MaxUploadSizeExceededException ex, RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("errorMessage", String.format("File uploads cannot be larger than %sMB.",
				this.multipartProperties.getMaxFileSize().toMegabytes()));
		return "redirect:/upload-error";
	}

	@ExceptionHandler(MultipartException.class)
	public String multipartErrorHandler(MultipartException ex, RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("errorMessage", "An unexpected exception occured.");
		return "redirect:/upload-error";
	}
}
