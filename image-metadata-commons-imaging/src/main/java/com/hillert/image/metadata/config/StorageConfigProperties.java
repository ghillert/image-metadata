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
package com.hillert.image.metadata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Storage-related Spring Boot configuration properties.
 *
 * @author Gunnar Hillert
 */
@ConfigurationProperties("storage")
public class StorageConfigProperties {

	/**
	 * Folder location for storing files.
	 */
	private String location = "upload-dir";

	public String getLocation() {
		return this.location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

}
