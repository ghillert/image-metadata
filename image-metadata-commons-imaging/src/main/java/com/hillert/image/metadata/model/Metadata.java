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

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Holder for extracted image metadata. Inspired by the meta-data-extractor project:
 * <a href="https://github.com/drewnoakes/metadata-extractor">https://github.com/drewnoakes/metadata-extractor</a>
 * @author Gunnar Hillert
 */
public class Metadata {
	private final List<Directory> directories = new ArrayList<>();
	private GnssInfo gnssInfo;
	private String xmpData;

	public Iterable<Directory> getDirectories() {
		return this.directories;
	}

	public Map<DirectoryType, List<Directory>> getDirectoriesPerType() {
		final Map<DirectoryType, List<Directory>> directoryMap = new TreeMap<>();

		for (Directory directory : this.getDirectories()) {
			final List<Directory> directoriesByType = directoryMap.getOrDefault(directory.getDirectoryType(), new ArrayList<>());
			directoriesByType.add(directory);
			directoryMap.put(directory.getDirectoryType(), directoriesByType);
		}

		return directoryMap;
	}

	public int getDirectoryCount() {
		return this.directories.size();
	}

	@Override
	public String toString() {
		int count = getDirectoryCount();
		return String.format("Metadata (%d %s)",
				count,
				(count == 1) ? "directory" : "directories");
	}

	public void addDirectories(List<Directory> fileMetadata) {
		this.directories.addAll(fileMetadata);
	}

	public GnssInfo getGnssInfo() {
		return this.gnssInfo;
	}

	public void setGnssInfo(GnssInfo gnssInfo) {
		this.gnssInfo = gnssInfo;
	}

	public void setTimeZone(ZoneId timeZone) {
		if (this.gnssInfo == null) {
			return;
		}
		this.gnssInfo.setUserTimeZone(timeZone);
	}

	public void setXmpData(String xmpData) {
		this.xmpData = xmpData;
	}

	public String getXmpData() {
		return this.xmpData;
	}
}
