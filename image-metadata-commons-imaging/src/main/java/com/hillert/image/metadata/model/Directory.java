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

import java.util.Objects;

/**
 * Meta data category. See {@link DirectoryType}.
 *
 * @author Gunnar Hillert
 */
public class Directory implements Comparable<Directory> {

	@Override
	public int compareTo(Directory o) {
		return this.getClass().getSimpleName().compareTo(o.getClass().getSimpleName());
	}

	private final DirectoryType directoryType;

	private final String propertyName;

	private final String propertyValue;

	public Directory(DirectoryType directoryType, String propertyName, String propertyValue) {
		this.directoryType = directoryType;
		this.propertyName = propertyName;
		this.propertyValue = propertyValue;
	}

	public DirectoryType getDirectoryType() {
		return this.directoryType;
	}

	public String getPropertyName() {
		return this.propertyName;
	}

	public String getPropertyValue() {
		return this.propertyValue;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Directory directory = (Directory) o;
		return this.directoryType == directory.directoryType && this.propertyName.equals(directory.propertyName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.directoryType, this.propertyName);
	}

}
