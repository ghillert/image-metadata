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
package com.hillert.image.metadata.service.support;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import com.hillert.image.metadata.model.Directory;
import com.hillert.image.metadata.model.DirectoryType;
import com.hillert.image.metadata.model.GnssInfo;
import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.GenericImageMetadata;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.common.RationalNumber;
import org.apache.commons.imaging.formats.gif.GifImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegPhotoshopMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants;
import org.apache.xmlgraphics.util.QName;
import org.apache.xmlgraphics.xmp.XMPParser;
import org.apache.xmlgraphics.xmp.XMPProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Utility class for extracting metadata from image data provided by {@link Resource}s.
 * @author Gunnar Hillert
 */
public final class MetadataExtractor {

	private static final Logger LOGGER = LoggerFactory.getLogger(MetadataExtractor.class);

	private MetadataExtractor() {
		throw new AssertionError("This is a static utility class.");
	}

	public static List<Directory> getFileMetadata(Resource resource) {

		final List<Directory> directories = new ArrayList<>();

		try {
			BasicFileAttributes attr = Files.readAttributes(resource.getFile().toPath(), BasicFileAttributes.class);
			directories.add(new Directory(DirectoryType.FILE, "File Name", resource.getFilename()));
			directories.add(new Directory(DirectoryType.FILE, "File Size", String.valueOf(CommonUtils.humanReadableByteCountSI(attr.size()))));
			directories.add(new Directory(DirectoryType.FILE, "File Modification Date/Time", String.valueOf(attr.creationTime())));
			directories.add(new Directory(DirectoryType.FILE, "File Creation Date/Time", String.valueOf(attr.lastModifiedTime())));
		}
		catch (FileNotFoundException ex) {
			return directories;
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		return directories;
	}

	public static List<Directory> getXMPMetadata(Resource resource) {

		final List<Directory> directories = new ArrayList<>();
		final String xmpString;
		try {
			xmpString = Imaging.getXmpXml(resource.getInputStream(), resource.getFilename());
		}
		catch (ImageReadException ex) {
			throw new ImageProcessingException("Unable to read image data.", ex);
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}

		if (!StringUtils.hasText(xmpString)) {
			return directories;
		}

		LOGGER.info(xmpString);

		try {
			//org.apache.xmlgraphics.xmp.Metadata xmpMetaData = XMPParser.parseXMP(new StreamSource(new StringReader(xmpString)));

			org.apache.xmlgraphics.xmp.Metadata xmpMetaData = XMPParser.parseXMP(new StreamSource(new StringReader(xmpString.substring(0, xmpString.lastIndexOf('>') + 1))));

			for (Iterator it = xmpMetaData.iterator(); it.hasNext(); ) {
				QName key = (QName) it.next();
				XMPProperty property = xmpMetaData.getProperty(key);
				final String propertyValue;
				if (property.isArray()) {
					propertyValue = (property.getArrayValue().getSimpleValue() != null) ? property.getArrayValue().getSimpleValue().toString() : "N/A";
				}
				else {
					propertyValue = property.getValue().toString();
				}
				directories.add(new Directory(DirectoryType.XMP, property.getName().getLocalName(), propertyValue));
			}
			LOGGER.info("XMP Property Count: {}", xmpMetaData.getPropertyCount());
		}
		catch (TransformerException ex) {
			throw new IllegalStateException(ex); //TODO
		}
		return directories;
	}

	public static List<Directory> getImageInfo(Resource resource) {

		final List<Directory> directories = new ArrayList<>();

		final ImageInfo imageInfo;

		try {
			imageInfo = Imaging.getImageInfo(resource.getInputStream(), resource.getFilename());
		}
		catch (ImageReadException ex) {
			throw new RuntimeException(ex);
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		final int bitsPerPixel = imageInfo.getBitsPerPixel();
		final String comments = StringUtils.collectionToDelimitedString(imageInfo.getComments(), " ");
		final String colorType = imageInfo.getColorType().name();
		final int numberOfImages = imageInfo.getNumberOfImages();
		final String compressionAlgorith = imageInfo.getCompressionAlgorithm().name();
		final String format = imageInfo.getFormat().getName();
		final String formatDetails = imageInfo.getFormatDetails();
		final int height = imageInfo.getHeight();
		final int width = imageInfo.getWidth();
		final int physicalHeightDpi = imageInfo.getPhysicalHeightDpi();
		final float physicalHeightInch = imageInfo.getPhysicalHeightInch();
		final int physicalWidthDpi = imageInfo.getPhysicalWidthDpi();
		final float physicalWidthInc = imageInfo.getPhysicalWidthInch();
		final boolean isProgressive = imageInfo.isProgressive();
		final String mimeType = imageInfo.getMimeType();
		final boolean isTransperent = imageInfo.isTransparent();
		final boolean usesPalette = imageInfo.usesPalette();

		directories.add(new Directory(DirectoryType.FILE_INFO, "bitsPerPixel", String.valueOf(bitsPerPixel)));
		directories.add(new Directory(DirectoryType.FILE_INFO, "comments", comments));
		directories.add(new Directory(DirectoryType.FILE_INFO, "colorType", colorType));
		directories.add(new Directory(DirectoryType.FILE_INFO, "numberOfImages", String.valueOf(numberOfImages)));
		directories.add(new Directory(DirectoryType.FILE_INFO, "compressionAlgorith", compressionAlgorith));
		directories.add(new Directory(DirectoryType.FILE_INFO, "format", format));
		directories.add(new Directory(DirectoryType.FILE_INFO, "formatDetails", formatDetails));
		directories.add(new Directory(DirectoryType.FILE_INFO, "height", String.valueOf(height)));
		directories.add(new Directory(DirectoryType.FILE_INFO, "width", String.valueOf(width)));
		directories.add(new Directory(DirectoryType.FILE_INFO, "physicalHeightDpi", String.valueOf(physicalHeightDpi)));
		directories.add(new Directory(DirectoryType.FILE_INFO, "physicalHeightInch", String.valueOf(physicalHeightInch)));
		directories.add(new Directory(DirectoryType.FILE_INFO, "physicalWidthDpi", String.valueOf(physicalWidthDpi)));
		directories.add(new Directory(DirectoryType.FILE_INFO, "physicalWidthInc", String.valueOf(physicalWidthInc)));
		directories.add(new Directory(DirectoryType.FILE_INFO, "isProgressive", String.valueOf(isProgressive)));
		directories.add(new Directory(DirectoryType.FILE_INFO, "mimeType", mimeType));
		directories.add(new Directory(DirectoryType.FILE_INFO, "isTransperent", String.valueOf(isTransperent)));
		directories.add(new Directory(DirectoryType.FILE_INFO, "usesPalette", String.valueOf(usesPalette)));

		return directories;
	}

	public static ImageMetadata getImageMetadata(Resource resource) {
		final ImageMetadata metadata;
		try {
			metadata = Imaging.getMetadata(resource.getInputStream(), resource.getFilename());
		}
		catch (ImageReadException ex) {
			throw new RuntimeException(ex);
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		return metadata;
	}

	private static DateTimeFormatter GPS_LOCAL_DATE = new DateTimeFormatterBuilder()
				.appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
				.appendLiteral(':')
				.appendValue(ChronoField.MONTH_OF_YEAR, 2)
				.appendLiteral(':')
				.appendValue(ChronoField.DAY_OF_MONTH, 2)
				.toFormatter(Locale.US);

	public static List<Directory> getJpegImageMetadata(JpegImageMetadata jpegImageMetadata) {
		final List<Directory> directories = new ArrayList<>();

		if (jpegImageMetadata == null) {
			return directories;
		}

		TiffImageMetadata exifMetadata = jpegImageMetadata.getExif();

		if (exifMetadata != null) {
			final List<TiffImageMetadata.TiffMetadataItem> exifMetadataItems = (List<TiffImageMetadata.TiffMetadataItem>) exifMetadata.getItems();

			for (TiffImageMetadata.TiffMetadataItem tiffMetadataItem : exifMetadataItems) {
				String value = StringUtils.hasText(tiffMetadataItem.getText()) ? tiffMetadataItem.getText() : "N/A";
				LOGGER.info(">>> : " + tiffMetadataItem.getTiffField() + " : " + value); //TODO
				directories.add(new Directory(DirectoryType.EXIF, tiffMetadataItem.getKeyword(), value));
			}
		}

//		GenericImageMetadata.GenericImageMetadataItem genericImageMetadataItem = new GenericImageMetadata.GenericImageMetadataItem();
//		jpegPhotoshopMetadata.add();

//		for (ImageMetadata.ImageMetadataItem imageMetadataItem : jpegMetadata.getItems()) {
		JpegPhotoshopMetadata jpegPhotoshopMetadata = jpegImageMetadata.getPhotoshop();

		if (jpegPhotoshopMetadata != null) {
			for (ImageMetadata.ImageMetadataItem imageMetadataItem : jpegPhotoshopMetadata.getItems()) {
				if (imageMetadataItem instanceof GenericImageMetadata.GenericImageMetadataItem) {
					GenericImageMetadata.GenericImageMetadataItem genericItem = (GenericImageMetadata.GenericImageMetadataItem) imageMetadataItem;
					LOGGER.info(genericItem.getKeyword() + " : " + genericItem.getText()); //TODO
					directories.add(new Directory(DirectoryType.IPTC, genericItem.getKeyword(), genericItem.getText()));
				}
				else {
					System.out.println(imageMetadataItem.getClass().getSimpleName()); //TODO
				}
			}
		}

		final TiffImageMetadata exif = jpegImageMetadata.getExif();
		if (exif == null) {
			return directories;
		}

		for (TiffField tiffField : exif.getAllFields()) {
			LOGGER.info("TIFF field tag name: {}", tiffField.getTagName());
		}

		return directories;
	}

	public static List<Directory> getGifImageMetadata(GifImageMetadata gifImageMetadata) {
		final List<Directory> directories = new ArrayList<>();

		directories.add(new Directory(DirectoryType.GIF, "height", String.valueOf(gifImageMetadata.getHeight())));
		directories.add(new Directory(DirectoryType.GIF, "width", String.valueOf(gifImageMetadata.getWidth())));

		return directories;
	}

	public static GnssInfo getGnssMetadata(JpegImageMetadata jpegImageMetadata) {

		try {

			if (jpegImageMetadata.getExif() == null || jpegImageMetadata.getExif().getGPS() == null) {
				return null;
			}

			final TiffImageMetadata exifMetadata = jpegImageMetadata.getExif();
			final TiffImageMetadata.GPSInfo gpsInfo = exifMetadata.getGPS();

			final TiffField altitudeField = exifMetadata.findField(GpsTagConstants.GPS_TAG_GPS_ALTITUDE);
			altitudeField.getDoubleValue();
			final TiffField altitudeRefField = exifMetadata.findField(GpsTagConstants.GPS_TAG_GPS_ALTITUDE_REF);
			altitudeRefField.getIntValue();

			final TiffField datestampField = exifMetadata.findField(GpsTagConstants.GPS_TAG_GPS_DATE_STAMP);
			final String datestamp = datestampField.getStringValue();
			final LocalDate localDate = LocalDate.parse(datestamp, GPS_LOCAL_DATE);

			final TiffField timestampField = exifMetadata.findField(GpsTagConstants.GPS_TAG_GPS_TIME_STAMP);
			final RationalNumber[] timestamp = (RationalNumber[]) timestampField.getValue();
			Assert.isTrue(timestamp.length == 3, "The timestamp should have a length of 3 but was " + timestamp.length);
			final LocalTime localTime = LocalTime.of(timestamp[0].intValue(), timestamp[1].intValue(), timestamp[2].intValue());

			final LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime);
			ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneOffset.UTC);

			final GnssInfo gnssInfo = new GnssInfo(gpsInfo.getLatitudeAsDegreesNorth(), gpsInfo.getLongitudeAsDegreesEast());
			final TiffField elevationField = exifMetadata.findField(GpsTagConstants.GPS_TAG_GPS_ALTITUDE);
			gnssInfo.setElevation(elevationField.getDoubleValue());
			gnssInfo.setGnssTime(zonedDateTime);
			return gnssInfo;

		}
		catch (ImageReadException ex) {
			throw new RuntimeException(ex);
		}
	}
}
