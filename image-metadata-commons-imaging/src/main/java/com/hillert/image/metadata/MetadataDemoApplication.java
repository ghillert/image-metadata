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
package com.hillert.image.metadata;

import com.hillert.image.metadata.service.ImageService;
import com.hillert.image.metadata.service.StorageProperties;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * Entrypoint into the demo application.
 * @author Gunnar Hillert
 */
@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
@ImportRuntimeHints(MyRuntimeHints.class)
public class MetadataDemoApplication {

	public static void main(String[] args) {
		SpringApplicationBuilder builder = new SpringApplicationBuilder(MetadataDemoApplication.class);
		builder.headless(true)
			.run(args);
	}

	@Bean
	ApplicationRunner init(ImageService imageService) {
		return (args) -> {
			imageService.deleteAll();
			imageService.init();
		};
	}
}
