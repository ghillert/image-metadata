/*
 * Copyright (c) 2023, 2024 Gunnar Hillert.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import com.hillert.image.metadata.model.DirectoryType;
import com.hillert.image.metadata.model.GnssInfo;
import com.hillert.image.metadata.model.Metadata;
import com.hillert.image.metadata.service.support.CommonUtils;
import com.hillert.image.metadata.service.support.ImageProcessingException;
import com.hillert.image.metadata.service.support.MetadataExtractor;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.ImagingException;
import org.apache.commons.imaging.common.GenericImageMetadata;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.gif.GifImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegPhotoshopMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.jpeg.iptc.IptcBlock;
import org.apache.commons.imaging.formats.jpeg.iptc.IptcRecord;
import org.apache.commons.imaging.formats.jpeg.iptc.IptcTypes;
import org.apache.commons.imaging.formats.jpeg.iptc.JpegIptcRewriter;
import org.apache.commons.imaging.formats.jpeg.iptc.PhotoshopApp13Data;
import org.apache.commons.imaging.formats.jpeg.xmp.JpegXmpRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.MicrosoftTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.apache.xmlgraphics.xmp.XMPParser;
import org.apache.xmlgraphics.xmp.XMPSerializer;
import org.apache.xmlgraphics.xmp.schemas.DublinCoreAdapter;
import org.apache.xmlgraphics.xmp.schemas.DublinCoreSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * Default implementation of the {@link MetadataService} interface.
 *
 * @author Gunnar Hillert
 */
@Service
public class DefaultMetadataService implements MetadataService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMetadataService.class);

	@Override
	public Metadata getExifData(Resource resource) {
		final Metadata metadataToReturn = new Metadata();

		metadataToReturn.addDirectories(MetadataExtractor.getFileMetadata(resource));
		metadataToReturn.addDirectories(MetadataExtractor.getXMPMetadata(resource));
		metadataToReturn.addDirectories(MetadataExtractor.getImageInfo(resource));

		final ImageMetadata metadata = MetadataExtractor.getImageMetadata(resource);

		if (metadata == null) {
			return metadataToReturn;
		}

		if (metadata instanceof JpegImageMetadata jpegImageMetadata) {
			metadataToReturn.addDirectories(MetadataExtractor.getJpegImageMetadata(jpegImageMetadata));
			final GnssInfo gnssInfo = MetadataExtractor.getGnssMetadata(jpegImageMetadata);
			metadataToReturn.setGnssInfo(gnssInfo);
		}
		else if (metadata instanceof GenericImageMetadata genericImageMetadata) {
			metadataToReturn.addDirectories(MetadataExtractor.getGenericImageMetadata(genericImageMetadata));
		}
		else if (metadata instanceof GifImageMetadata gifImageMetadata) {
			metadataToReturn.addDirectories(MetadataExtractor.getGifImageMetadata(gifImageMetadata));
		}
		else {
			throw new IllegalStateException("Unsupported metadata type " + metadata.getClass().getSimpleName());
		}

		final String xmpXmlData;
		try {
			xmpXmlData = this.getXmpXml(StreamUtils.copyToByteArray(resource.getInputStream()));
		}
		catch (IOException ex) {
			throw new ImageProcessingException("Unable to get Inputstream from resource " + resource.getFilename(), ex);
		}
		if (xmpXmlData != null) {
			metadataToReturn.setXmpData(xmpXmlData);
		}
		return metadataToReturn;
	}

	/**
	 * This method will purge the image metadata for the provided metadataType. The
	 * following 3 types are supported:
	 * <ul>
	 * <li>{@link DirectoryType#EXIF}
	 * <li>{@link DirectoryType#IPTC}
	 * <li>{@link DirectoryType#XMP}
	 * </ul>
	 * See also consider https://issues.apache.org/jira/browse/IMAGING-132.
	 * @param imageBytes the image data to purge metadata from
	 * @param metadataType which type of metadata to purge
	 * @return the modified image data #throws ImageProcessingException in case an
	 * unsupported {@link DirectoryType} is proided
	 */
	@Override
	public byte[] purge(byte[] imageBytes, DirectoryType metadataType) {

		LOGGER.info("Remove metadata for metadataType {}", metadataType.getName());

		final byte[] purgedImageData;

		try (ByteArrayOutputStream os = new ByteArrayOutputStream();) {
			switch (metadataType) {
				case EXIF -> new ExifRewriter().removeExifMetadata(imageBytes, os);
				case IPTC -> new JpegIptcRewriter().removeIptc(imageBytes, os);
				case XMP -> new JpegXmpRewriter().removeXmpXml(imageBytes, os);
				default ->
					throw new ImageProcessingException("Unsupported DirectoryType " + metadataType.getName(), null);
			}
			purgedImageData = os.toByteArray();
		}
		catch (IOException ex) {
			throw new ImageProcessingException("Unable to purge Image metadata for " + metadataType.getName(), ex);
		}
		return purgedImageData;
	}

	@Override
	public byte[] updateMetadata(byte[] imageBytes, boolean populateWindowsTags, String referenceId, String title) {

		LOGGER.info("populateWindowsTags: {}; referenceId: {}; title: {}", populateWindowsTags, referenceId, title);

		final HashMap<String, String> exifTagsToPopulate = new HashMap<>();
		final HashMap<String, String> iptcTagsToPopulate = new HashMap<>();
		final HashMap<String, String> xmpTagsToPopulate = new HashMap<>();

		if (StringUtils.hasText(referenceId)) {
			iptcTagsToPopulate.put("referenceId", referenceId);
			xmpTagsToPopulate.put("referenceId", referenceId);
		}

		if (StringUtils.hasText(title)) {
			exifTagsToPopulate.put("imageDescription", title);
			iptcTagsToPopulate.put("objectName", title);
			xmpTagsToPopulate.put("title", title);

			if (populateWindowsTags) {
				exifTagsToPopulate.put("xptitle", title);
			}
		}

		byte[] resultImageBytes = populateExifTags(imageBytes, exifTagsToPopulate);
		resultImageBytes = populateIptcTags(resultImageBytes, iptcTagsToPopulate);
		resultImageBytes = populateXmpTags(resultImageBytes, xmpTagsToPopulate);

		return resultImageBytes;
	}

	private byte[] populateExifTags(byte[] imageBytes, HashMap<String, String> exifTagsToPopulate) {
		LOGGER.info("Populating EXIF Tags.");

		final TiffOutputSet outputSet;

		try {
			ImageMetadata metadata = Imaging.getMetadata(imageBytes);

			// if file does not contain any exif metadata, we create an empty
			// set of exif metadata. Otherwise, we keep all of the other
			// existing tags.
			if (metadata == null) {
				outputSet = new TiffOutputSet();
			}
			else if (metadata instanceof JpegImageMetadata jpegMetadata) {
				final TiffImageMetadata exif = jpegMetadata.getExif();
				if (null != exif) {
					outputSet = exif.getOutputSet();
				}
				else {
					outputSet = new TiffOutputSet();
				}
			}
			else {
				LOGGER.warn("Ignoring ImageMetadata {}.", metadata.getClass().getSimpleName());
				outputSet = new TiffOutputSet();
			}
		}
		catch (ImagingException ex) {
			throw new ImageProcessingException("Unable to get EXIF Tags.", ex);
		}
		catch (IOException ex) {
			throw new ImageProcessingException("Unable to get Image Metadata.", ex);
		}

		final byte[] modifiedImageData;

		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			for (Map.Entry<String, String> tag : exifTagsToPopulate.entrySet()) {
				if ("imageDescription".equalsIgnoreCase(tag.getKey())) {
					final TiffOutputDirectory rootDir = outputSet.getOrCreateRootDirectory();
					rootDir.removeField(TiffTagConstants.TIFF_TAG_IMAGE_DESCRIPTION);
					rootDir.add(TiffTagConstants.TIFF_TAG_IMAGE_DESCRIPTION, tag.getValue());
				}

				if ("xptitle".equalsIgnoreCase(tag.getKey())) {
					final TiffOutputDirectory rootDir = outputSet.getOrCreateRootDirectory();
					rootDir.removeField(MicrosoftTagConstants.EXIF_TAG_XPTITLE);
					rootDir.add(MicrosoftTagConstants.EXIF_TAG_XPTITLE, tag.getValue());
				}
			}

			// TODO
			// rootDir.removeField(MicrosoftTagConstants.EXIF_TAG_XPSUBJECT);
			// rootDir.add(MicrosoftTagConstants.EXIF_TAG_XPSUBJECT, "new subject");
			//
			// rootDir.removeField(MicrosoftTagConstants.EXIF_TAG_XPCOMMENT);
			// rootDir.add(MicrosoftTagConstants.EXIF_TAG_XPCOMMENT, "new comment");
			//
			// rootDir.removeField(MicrosoftTagConstants.EXIF_TAG_XPKEYWORDS);
			// rootDir.add(MicrosoftTagConstants.EXIF_TAG_XPKEYWORDS, "key1;key2");
			//
			// rootDir.removeField(MicrosoftTagConstants.EXIF_TAG_RATING);
			// rootDir.add(MicrosoftTagConstants.EXIF_TAG_RATING, (short) 4);

			new ExifRewriter().updateExifMetadataLossless(imageBytes, os, outputSet);
			modifiedImageData = os.toByteArray();
		}
		catch (IOException ex) {
			throw new ImageProcessingException("Unable to populate Windows-specific EXIF Tags.", ex);
		}
		return modifiedImageData;
	}

	private byte[] populateIptcTags(byte[] imageBytes, HashMap<String, String> iptcTagsToPopulate) {
		LOGGER.info("Populating {} IPTC Tags.", iptcTagsToPopulate.size());

		if (iptcTagsToPopulate.isEmpty()) {
			return imageBytes;
		}

		final List<IptcBlock> newBlocks;
		final List<IptcRecord> newRecords;

		try {
			final ImageMetadata metadata = Imaging.getMetadata(imageBytes);
			if (metadata == null) {
				newBlocks = new ArrayList<>();
				newRecords = new ArrayList<>();
			}
			else {
				final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
				final JpegPhotoshopMetadata jpegPhotoshopMetadata = jpegMetadata.getPhotoshop();
				if (jpegPhotoshopMetadata != null) {
					newRecords = jpegPhotoshopMetadata.photoshopApp13Data.getRecords();
					newBlocks = jpegPhotoshopMetadata.photoshopApp13Data.getNonIptcBlocks();
				}
				else {
					newBlocks = new ArrayList<>();
					newRecords = new ArrayList<>();
				}
			}
		}
		catch (IOException ex) {
			throw new ImageProcessingException("Unable to get Image Metadata.", ex);
		}

		final byte[] modifiedImageData;

		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			for (Map.Entry<String, String> tag : iptcTagsToPopulate.entrySet()) {
				if ("referenceId".equalsIgnoreCase(tag.getKey())) {
					newRecords
						.removeIf((p) -> p.iptcType.getType() == IptcTypes.ORIGINAL_TRANSMISSION_REFERENCE.getType());
					newRecords.add(new IptcRecord(IptcTypes.ORIGINAL_TRANSMISSION_REFERENCE, tag.getValue()));
				}
				else if ("objectName".equalsIgnoreCase(tag.getKey())) {
					newRecords
						.removeIf((p) -> p.iptcType.getType() == IptcTypes.OBJECT_NAME.getType());
					newRecords.add(new IptcRecord(IptcTypes.OBJECT_NAME, tag.getValue()));
				}
			}
			final PhotoshopApp13Data newData = new PhotoshopApp13Data(newRecords, newBlocks);
			new JpegIptcRewriter().writeIptc(imageBytes, os, newData);
			modifiedImageData = os.toByteArray();
		}
		catch (IOException ex) {
			throw new ImageProcessingException("Unable to populate reference id.", ex);
		}
		return modifiedImageData;
	}

	private byte[] populateXmpTags(byte[] imageBytes, HashMap<String, String> xmpTagsToPopulate) {

		if (xmpTagsToPopulate.isEmpty()) {
			return imageBytes;
		}

		final String xmpXml = getXmpXml(imageBytes);
		final org.apache.xmlgraphics.xmp.Metadata xmpMetaData;

		if (xmpXml == null) {
			xmpMetaData = new org.apache.xmlgraphics.xmp.Metadata();
		}
		else {
			final ByteArrayInputStream input = new ByteArrayInputStream(xmpXml.getBytes(StandardCharsets.UTF_8));
			try {
				xmpMetaData = XMPParser.parseXMP(new StreamSource(input));
			}
			catch (TransformerException ex) {
				throw new ImageProcessingException("Unable to parse XMP data.", ex);
			}
		}
		final DublinCoreAdapter dc = DublinCoreSchema.getAdapter(xmpMetaData);

		for (Map.Entry<String, String> entry : xmpTagsToPopulate.entrySet()) {
			if ("referenceId".equalsIgnoreCase(entry.getKey())) {
				dc.setIdentifier(entry.getValue());
			}
			if ("title".equalsIgnoreCase(entry.getKey())) {
				dc.setTitle(entry.getValue());
			}
		}

		final String xmpAsString;

		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			XMPSerializer.writeXMPPacket(xmpMetaData, bos, false);
			xmpAsString = bos.toString(StandardCharsets.UTF_8);
		}
		catch (TransformerConfigurationException | SAXException | IOException ex) {
			throw new ImageProcessingException("Unable to write XMP data.", ex);
		}

		final byte[] modifiedImageData;
		try (ByteArrayOutputStream modifiedImageDataOutputStream = new ByteArrayOutputStream()) {
			new JpegXmpRewriter().updateXmpXml(imageBytes, modifiedImageDataOutputStream, xmpAsString);
			modifiedImageData = modifiedImageDataOutputStream.toByteArray();
		}
		catch (IOException ex) {
			throw new ImageProcessingException("Unable to write XMP data to JPG.", ex);
		}
		return modifiedImageData;
	}

	private String getXmpXml(byte[] imageBytes) {
		final String xmpString;
		try {
			xmpString = Imaging.getXmpXml(imageBytes);
		}
		catch (ImagingException ex) {
			throw new ImageProcessingException("Unable to parse the image.", ex);
		}
		catch (IOException ex) {
			throw new ImageProcessingException("Unable to read the image data.", ex);
		}
		if (xmpString == null) {
			return null;
		}

		return CommonUtils.formatXml(xmpString, false);
	}

}
