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
package com.hillert.image.metadata.service;

import com.hillert.image.metadata.model.DirectoryType;
import com.hillert.image.metadata.model.Metadata;

import org.springframework.core.io.Resource;

/**
 * Provides methods to read and update image metadata.
 *
 * @author Gunnar Hillert
 */
public interface MetadataService {

	/**
	 * Retrieve the metadata for a provide image {@link Resource}.
	 * @param resource must not be null.
	 * @return the extracted image data. Never null.
	 */
	Metadata getExifData(Resource resource);

	/**
	 * Removes the image metadata of an image.
	 * @param imageBytes the image data. Must not be null.
	 * @param metadataType for which metadata type shall the image metadata removed?
	 * @return the image data without the purge metadata
	 */
	byte[] purge(byte[] imageBytes, DirectoryType metadataType);

	/**
	 * Updating the metadata can get a bit messy, particularly for the caption as we need
	 * to store (being good citizens) the caption in 3 locations:
	 *
	 * <ul>
	 * <li>EXIF - ImageDescription tag</li>
	 * <li>IPTC - Caption-Abstract tag</li>
	 * <li>XMP - Description tag</li>
	 * </ul>
	 *
	 * If #populateWindowsTags is true, the caption will also populate the EXIF XPTitle
	 * tag.
	 *
	 * The referenceId can be used to connect the image to e.g. your application data.
	 * This data needs to be stored in:
	 *
	 * <ul>
	 * <li>IPTC - OriginalTransmissionReference tag</li>
	 * <li>XMP (Dublin Core) - Identifier</li>
	 * </ul>
	 *
	 * TODO We should probably also populate the TransmissionReference tag from the XMP
	 * (IPTC / Photoshop) namespace but Apache XML Graphics Commons supports Dublin Core
	 * only.
	 *
	 * I created: https://issues.apache.org/jira/browse/XGC-135 to "Add XMP support for
	 * IPTC Photo Metadata Standard"
	 *
	 * See also:
	 *
	 * <ul>
	 * <li>https://www.carlseibert.com/xmp-iptciim-or-exif-which-is-preferred/
	 * <li>https://exiftool.org/TagNames/EXIF.html
	 * <li>https://exiftool.org/TagNames/IPTC.html
	 * <li>https://exiftool.org/TagNames/XMP.html
	 * <li>https://www.iptc.org/std/photometadata/specification/IPTC-PhotoMetadata
	 * <li>https://www.iptc.org/std/photometadata/specification/IPTC-PhotoMetadata-201007.pdf
	 * </ul>
	 * @param imageBytes the image to update the metadata for
	 * @param populateWindowsTags if true, update MS Windows specific metadata tags
	 * @param referenceId a reference id to tag your image with. Can be null.
	 * @param caption the title of your image. Can be null.
	 * @return the image data with the updated metadata.
	 */
	byte[] updateMetadata(byte[] imageBytes, boolean populateWindowsTags, String referenceId, String caption);

	/**
	 * Retrieve the image description for a provided image {@link Resource}.
	 * The image description will be retrieved using AI and then store as a text file in the same directory as the image
	 * file. Therefore, multiple requests for the same image will return the same description. Delete the text file to
	 * generate a new image description.
	 * @param resource must not be null
	 * @return the description for the provided image
	 */
	String getImageDescription(Resource resource);
}
