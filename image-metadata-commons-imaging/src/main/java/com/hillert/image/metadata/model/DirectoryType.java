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
package com.hillert.image.metadata.model;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.util.Assert;

/**
 * An enum representing the various metadata categories that are supported.
 * @author Gunnar Hillert
 * @see Directory
 */
public enum DirectoryType {

	/**
	 * IPTC-related metadata.
	 */
	IPTC("IPTC", "International Press Telecommunications Council"),

	/**
	 * XMP-related metadata.
	 */
	XMP("XMP", "Extensible Metadata Platform"),

	/**
	 * EXIF-related metadata.
	 */
	EXIF("EXIF", "Exchangeable Image File Format"),

	/**
	 * Property data of the file.
	 */
	FILE("File", "Image File Properties"),

	/**
	 * Basic information of the file itself.
	 */
	FILE_INFO("File Info", "Image Format Info"),

	/**
	 * GIF related metadata.
	 */
	GIF("GIF", "Graphics Interchange Format");

	private String name;
	private String description;

	/**
	 * Constructor.
	 * @param name of the directory type
	 * @param description short description of the directory
	 */
	DirectoryType(final String name, final String description) {
		this.name = name;
		this.description = description;
	}

	public String getName() {
		return this.name;
	}

	public String getDescription() {
		return this.description;
	}

	public static DirectoryType fromName(final String name) {

		Assert.hasText(name, "Parameter name, must not be null or empty.");

		for (DirectoryType directoryType : DirectoryType.values()) {
			if (directoryType.getName().equals(name)) {
				return directoryType;
			}
		}
		return null;
	}

	public static List<DirectoryType> getValuesAsList() {
		return Stream.of(DirectoryType.values())
		.sorted(Comparator.comparing(DirectoryType::getName))
		.toList();
	}
}
