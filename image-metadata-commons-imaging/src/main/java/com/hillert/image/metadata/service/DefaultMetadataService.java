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
package com.hillert.image.metadata.service;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.hillert.image.metadata.model.DirectoryType;
import com.hillert.image.metadata.model.GnssInfo;
import com.hillert.image.metadata.model.Metadata;
import com.hillert.image.metadata.service.support.ImageProcessingException;
import com.hillert.image.metadata.service.support.MetadataExtractor;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
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
import org.apache.commons.io.IOUtils;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * Default implementation of the {@link MetadataService} interface.
 * @author Gunnar Hillert
 */
@Service
public class DefaultMetadataService implements MetadataService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMetadataService.class);

	@Override
	public Metadata getExifData(Resource resource) {
		Metadata metadataToReturn = new Metadata();

		metadataToReturn.addDirectories(MetadataExtractor.getFileMetadata(resource));
		metadataToReturn.addDirectories(MetadataExtractor.getXMPMetadata(resource));
		metadataToReturn.addDirectories(MetadataExtractor.getImageInfo(resource));

		final ImageMetadata metadata = MetadataExtractor.getImageMetadata(resource);

		if (metadata == null) {
			return metadataToReturn;
		}

		if (metadata instanceof JpegImageMetadata jpegImageMetadata) {
			metadataToReturn.addDirectories(MetadataExtractor.getJpegImageMetadata(jpegImageMetadata));
			GnssInfo gnssInfo = MetadataExtractor.getGnssMetadata(jpegImageMetadata);
			metadataToReturn.setGnssInfo(gnssInfo);
		}
		else if (metadata instanceof GifImageMetadata gifImageMetadata) {
			metadataToReturn.addDirectories(MetadataExtractor.getGifImageMetadata(gifImageMetadata));
		}
		else {
			throw new IllegalStateException("Unsupported metadata type " + metadata.getClass().getSimpleName());
		}

		final String xmpXmlData;
		try {
			xmpXmlData = this.getXmpXml(getBytes(resource.getInputStream()));
		}
		catch (IOException ex) { //TODO
			throw new ImageProcessingException("Unable to get Inputstream from resource " + resource.getFilename(), ex);
		}
		metadataToReturn.setXmpData(xmpXmlData);
		return metadataToReturn;
	}

	//TODO try apache commons io
	private static byte[] getBytes(InputStream is) throws IOException {
		byte[] buffer = new byte[8192];
		ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
		int n;
		baos.reset();

		while ((n = is.read(buffer, 0, buffer.length)) != -1) {
			baos.write(buffer, 0, n);
		}

		return baos.toByteArray();
	}

	/**
	 * May also consider https://issues.apache.org/jira/browse/IMAGING-132.
	 * @param imageBytes the image data to purge metadata from
	 * @param metadataType which type of metadata to purge
	 * @return the modified image data
	 */
	@Override
	public byte[] purge(byte[] imageBytes, DirectoryType metadataType) {

		LOGGER.info("Remove metadata for metadataType {}", metadataType.getName());

		ByteArrayOutputStream os = null;
		boolean canThrow = false;
		try {
			os = new ByteArrayOutputStream();

			switch (metadataType) {
				case EXIF -> new ExifRewriter().removeExifMetadata(imageBytes, os);
				case IPTC -> new JpegIptcRewriter().removeIPTC(imageBytes, os);
				case XMP -> new JpegXmpRewriter().removeXmpXml(imageBytes, os);
				default -> throw new ImageProcessingException("Unsupported DirectoryType " + metadataType.getName(), null);
			}
		}
		catch (ImageWriteException | IOException | ImageReadException ex) {
			throw new ImageProcessingException("Unable to purge Image metadata for " + metadataType.getName(), ex);
		}
		finally {
			IOUtils.closeQuietly(os);
		}
		return os.toByteArray();
	}

	public static boolean updateWindowsFields(final File jpegImageFile, final File dst)
			throws IOException, ImageReadException, ImageWriteException {

		try (FileOutputStream fos = new FileOutputStream(dst);
			OutputStream os = new BufferedOutputStream(fos)) {
			TiffOutputSet outputSet = null;
			final ImageMetadata metadata = Imaging.getMetadata(jpegImageFile);
			final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
			if (null != jpegMetadata) {

				final JpegPhotoshopMetadata jpegPhotoshopMetadata = jpegMetadata.getPhotoshop();
				final List<IptcBlock> newBlocks = jpegPhotoshopMetadata.photoshopApp13Data.getNonIptcBlocks();

				final List<IptcRecord> newRecords = new ArrayList<>();
				newRecords.add(new IptcRecord(IptcTypes.CITY, "Albany, NY"));
				newRecords.add(new IptcRecord(IptcTypes.CREDIT,
						"William Sorensen"));
				newRecords.add(new IptcRecord(IptcTypes.ORIGINAL_TRANSMISSION_REFERENCE,
						"ACESSION ID 1234"));
				newRecords.add(new IptcRecord(IptcTypes.SPECIAL_INSTRUCTIONS,
						"Pembana"));
				final PhotoshopApp13Data newData = new PhotoshopApp13Data(newRecords,
						newBlocks);
//				final File updated = writeIptc(byteSource, newData, imageFile);
//						new JpegIptcRewriter().writeIPTC();
				new JpegIptcRewriter().writeIPTC(jpegImageFile, os,
						newData);

//				new JpegImageParser().getXmpXml()
//				new JpegXmpParser().parseXmpJpegSegment()
//				new JpegXmpRewriter().updateXmpXml();

				// note that exif might be null if no Exif metadata is found.
				final TiffImageMetadata exif = jpegMetadata.getExif();
				if (null != exif) {
					outputSet = exif.getOutputSet();
				}
			}
			// if file does not contain any exif metadata, we create an empty
			// set of exif metadata. Otherwise, we keep all of the other
			// existing tags.
			if (null == outputSet) {
				outputSet = new TiffOutputSet();
			}

			final TiffOutputDirectory rootDir = outputSet.getOrCreateRootDirectory();
			rootDir.removeField(TiffTagConstants.TIFF_TAG_IMAGE_DESCRIPTION);
			rootDir.add(TiffTagConstants.TIFF_TAG_IMAGE_DESCRIPTION, "shit happens");

			rootDir.removeField(MicrosoftTagConstants.EXIF_TAG_XPTITLE);
			rootDir.add(MicrosoftTagConstants.EXIF_TAG_XPTITLE, "new title");

			rootDir.removeField(MicrosoftTagConstants.EXIF_TAG_XPSUBJECT);
			rootDir.add(MicrosoftTagConstants.EXIF_TAG_XPSUBJECT, "new subject");
			//
			rootDir.removeField(MicrosoftTagConstants.EXIF_TAG_XPCOMMENT);
			rootDir.add(MicrosoftTagConstants.EXIF_TAG_XPCOMMENT, "new comment");
			//
			rootDir.removeField(MicrosoftTagConstants.EXIF_TAG_XPKEYWORDS);
			rootDir.add(MicrosoftTagConstants.EXIF_TAG_XPKEYWORDS, "key1;key2");
			//
			rootDir.removeField(MicrosoftTagConstants.EXIF_TAG_RATING);
			rootDir.add(MicrosoftTagConstants.EXIF_TAG_RATING, (short) 4);

			new ExifRewriter().updateExifMetadataLossless(jpegImageFile, os,
					outputSet);
			return true;
		}
		catch (Exception ex) {
			return false;
		}
	}

	private String getXmpXml(byte[] imageBytes) {
		final String xmpString;
		try {
			xmpString = Imaging.getXmpXml(imageBytes);
		}
		catch (ImageReadException ex) {
			throw new ImageProcessingException("Unabled to parse the image.", ex);
		}
		catch (IOException ex) {
			throw new ImageProcessingException("Unable to read the image data.", ex);
		}

		ByteArrayInputStream input = new ByteArrayInputStream(imageBytes);

//		System.out.println(xmpString);
//		try {
//			org.apache.xmlgraphics.xmp.Metadata xmpMetaData = XMPParser.parseXMP(new StreamSource(input));
//		ByteArrayOutputStream bos = new ByteArrayOutputStream();
//		XMPSerializer.writeXMPPacket(xmpMetaData, bos, false );
//		} catch (TransformerException | SAXException e) {
//			throw new RuntimeException(e);
//		}
//		DublinCoreAdapter dc = DublinCoreSchema.getAdapter(xmpMetaData);
//		dc.setTitle("Shit happens!!!!");
//
//		XMPBasicAdapter basic = XMPBasicSchema.getAdapter(xmpMetaData);
//
//		ByteArrayOutputStream bos = new ByteArrayOutputStream();
//		XMPSerializer.writeXMPPacket(xmpMetaData, bos, false );
//
//		String f = new String( bos.toByteArray() );
//		System.out.println(f);
//		new JpegXmpRewriter().updateXmpXml(new File("/Users/hillert/dev/temp/123.jpg"), new FileOutputStream("/Users/hillert/dev/temp/123-eeee.jpg"), f);
		System.out.println("-----------");
		System.out.println(xmpString);
		System.out.println("-----------");
		System.out.println(format(xmpString, false));
		System.out.println("-----------");
		return format(xmpString, false);
	}

	public static String format(String xml, Boolean ommitXmlDeclaration) {
		DocumentBuilder db = null;
		try {
			db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		}
		catch (ParserConfigurationException ex) {
			throw new RuntimeException(ex);
		}
		Document doc = null;
		try {
			doc = db.parse(new InputSource(new StringReader(xml.trim())));
		}
		catch (SAXException ex) {
			throw new RuntimeException(ex);
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		OutputFormat format = new OutputFormat(doc);
		format.setIndenting(true);
		format.setIndent(2);
		format.setOmitXMLDeclaration(ommitXmlDeclaration);
		format.setLineWidth(Integer.MAX_VALUE);
		Writer outxml = new StringWriter();
		XMLSerializer serializer = new XMLSerializer(outxml, format);
		try {
			serializer.serialize(doc);
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		return outxml.toString();
	}
}
