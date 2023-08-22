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
package com.hillert.image.metadata.controller.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

/**
 * {@link ConstraintValidator} that ensures that a provided file is provided and that only
 * certain filetypes (mime-types) are supported.
 *
 * IMPORTANT: This is merely a convenience validator in case the mime-type is not
 * supported. If the mime-type is accepted, proper backend validation of the image data
 * shall still be performed.
 *
 * @author Gunnar Hillert
 */
public class ImageFileValidator implements ConstraintValidator<ValidImage, MultipartFile> {

	@Override
	public boolean isValid(MultipartFile multipartFile, ConstraintValidatorContext context) {

		boolean result = true;

		if (multipartFile.isEmpty()) {
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate("Please select a file.").addConstraintViolation();

			result = false;
			return result;
		}

		String contentType = multipartFile.getContentType();

		if (contentType == null) {
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate("Cannot retrieve content-type.").addConstraintViolation();

			result = false;
			return result;
		}

		/**
		 * Important: Keep in mind that the contentType can be faked. A deeper validation
		 * of the uploaded file should be done in the service layer.
		 */
		if (!isSupportedContentType(contentType)) {
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate("Only GIF, JPG, PNG images are allowed.")
				.addConstraintViolation();

			result = false;
		}

		return result;
	}

	private boolean isSupportedContentType(String contentType) {
		return contentType.equals(MediaType.IMAGE_PNG_VALUE) || contentType.equals(MediaType.IMAGE_GIF_VALUE)
				|| contentType.equals(MediaType.IMAGE_JPEG_VALUE);
	}

}
