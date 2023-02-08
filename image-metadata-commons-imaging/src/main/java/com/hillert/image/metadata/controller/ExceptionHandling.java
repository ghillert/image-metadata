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

import com.hillert.image.metadata.service.support.ImageProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Exception Handler.
 * @author Gunnar Hillert
 */
@ControllerAdvice
public class ExceptionHandling {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandling.class);

	private final MultipartProperties multipartProperties;

	public ExceptionHandling(MultipartProperties multipartProperties) {
		this.multipartProperties = multipartProperties;
	}

	@ExceptionHandler(ImageProcessingException.class)
	public String multipartErrorHandler(ImageProcessingException ex,
										RedirectAttributes redirectAttributes) throws Exception {
		redirectAttributes.addFlashAttribute("error", ex.getMessage());
		LOGGER.error("ImageProcessingException caught!", ex);
		return "redirect:/";
	}

	@ExceptionHandler(MultipartException.class)
	public String multipartErrorHandler(MultipartException ex,
					RedirectAttributes redirectAttributes) throws Exception {
		return "redirect:/uploadError";
	}
}
