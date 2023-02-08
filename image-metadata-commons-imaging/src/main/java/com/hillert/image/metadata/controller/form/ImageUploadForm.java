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
package com.hillert.image.metadata.controller.form;

import com.hillert.image.metadata.controller.validation.ValidImage;
import jakarta.validation.constraints.NotNull;

import org.springframework.web.multipart.MultipartFile;

/**
 * Contains form data when uploading an image file.
 * @author Gunnar Hillert
 */
public class ImageUploadForm {

	@NotNull
	@ValidImage
	private MultipartFile imageFile;
	private Boolean removeMetadata;

	public MultipartFile getImageFile() {
		return this.imageFile;
	}

	public void setImageFile(MultipartFile imageFile) {
		this.imageFile = imageFile;
	}

	public Boolean getRemoveMetadata() {
		return this.removeMetadata;
	}

	public void setRemoveMetadata(Boolean removeMetadata) {
		this.removeMetadata = removeMetadata;
	}
}
