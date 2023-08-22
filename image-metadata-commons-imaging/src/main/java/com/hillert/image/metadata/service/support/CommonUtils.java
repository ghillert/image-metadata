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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Contains common utilities for the project.
 *
 * @author Gunnar Hillert
 */
public final class CommonUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(CommonUtils.class);

	private CommonUtils() {
		throw new AssertionError("This is a static utility class.");
	}

	/**
	 * From:
	 * https://stackoverflow.com/questions/3758606/how-can-i-convert-byte-size-into-a-human-readable-format-in-java.
	 * @param bytes byte count that shall be converted to a human-readable format
	 * @return formatted byte-count
	 */
	public static String humanReadableByteCountSI(long bytes) {
		if (-1000 < bytes && bytes < 1000) {
			return bytes + " B";
		}
		CharacterIterator ci = new StringCharacterIterator("kMGTPE");
		while (bytes <= -999_950 || bytes >= 999_950) {
			bytes /= 1000;
			ci.next();
		}
		return String.format("%.1f %cB", bytes / 1000.0, ci.current());
	}

	public static byte[] fileToBytes(final File file) {
		Assert.notNull(file, "File must not be null.");
		Assert.isTrue(file.isFile(), "Provided file must exist and cannot be a directory.");
		try {
			return FileUtils.readFileToByteArray(file);
		}
		catch (IOException ex) {
			throw new IllegalStateException(String.format("Unable to read file '%s'", file.getAbsolutePath()), ex);
		}
	}

	public static byte[] inputStreamToBytes(final InputStream inputStream) {
		try {
			return IOUtils.toByteArray(inputStream);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to read inputStream", ex);
		}
	}

	public static byte[] resourceToBytes(final Resource resource) {
		Assert.notNull(resource, "Resource must not be null.");
		Assert.notNull(resource.isReadable(), "Resource must be readable.");
		try {
			return IOUtils.toByteArray(resource.getInputStream());
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to read inputStream from resource " + resource.getFilename(), ex);
		}
	}

	public static String formatXml(String xml, Boolean ommitXmlDeclaration) {
		final DocumentBuilder db;
		try {
			db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		}
		catch (ParserConfigurationException ex) {
			throw new IllegalStateException("Cannot build XML DocumentBuilder.", ex);
		}
		final Document doc;
		try {
			doc = db.parse(new InputSource(new StringReader(xml.trim())));
		}
		catch (SAXException | IOException ex) {
			throw new IllegalStateException("Cannot parse XML data.", ex);
		}

		@SuppressWarnings("deprecation")
		final OutputFormat format = new OutputFormat(doc);

		format.setIndenting(true);
		format.setIndent(2);
		format.setOmitXMLDeclaration(ommitXmlDeclaration);
		format.setLineWidth(Integer.MAX_VALUE);
		final Writer outXml = new StringWriter();

		@SuppressWarnings("deprecation")
		final XMLSerializer serializer = new XMLSerializer(outXml, format);
		try {
			serializer.serialize(doc);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Cannot serialize DOM document.", ex);
		}

		final String formattedXml = outXml.toString();

		LOGGER.info("\n-----------\n{}\n-----------\n", formattedXml);
		return formattedXml;
	}

}
