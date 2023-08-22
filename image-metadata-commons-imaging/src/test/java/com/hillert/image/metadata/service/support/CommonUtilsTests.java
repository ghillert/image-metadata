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

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gunnar Hillert
 */
class CommonUtilsTests {

	private final Resource rawXmlResource = new ClassPathResource("/rawXml.xml");

	private final Resource formattedXmlResource = new ClassPathResource("/formattedXml.xml");

	@Test
	void testTheFormattingOfXmlData() throws IOException {
		final String rawXmlAsString = IOUtils.toString(this.rawXmlResource.getInputStream(), StandardCharsets.UTF_8);
		final String formattedXml = CommonUtils.formatXml(rawXmlAsString, false);
		final String expectedXmlAsString = IOUtils.toString(this.formattedXmlResource.getInputStream(),
				StandardCharsets.UTF_8);
		assertThat(formattedXml).isNotNull().isEqualTo(expectedXmlAsString);
	}

}
