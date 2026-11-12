package com.hillert.image.metadata.service;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.hillert.image.metadata.config.MetadataConfigProperties;
import com.hillert.image.metadata.config.OpenAiConfigProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

import static org.assertj.core.api.Assertions.assertThat;

class OptionalAiTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withUserConfiguration(TestConfiguration.class)
		.withPropertyValues("metadata.ai.create-image-description=true", "metadata.ai.text-prompt=Describe this image.");

	@TempDir
	Path directory;

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " ", "\t" })
	void missingOrBlankKeyDisablesDescriptions(String key) {
		ApplicationContextRunner contextRunner = (key != null)
				? this.runner.withPropertyValues("spring.ai.openai.api-key=" + key) : this.runner;
		contextRunner.run((context) -> {
			assertThat(context).hasNotFailed().doesNotHaveBean(ChatModel.class).doesNotHaveBean(ChatClient.Builder.class);
			assertThat(context.getBean(MetadataService.class).getImageDescription(new ByteArrayResource(new byte[0])))
				.isEqualTo("Image descriptions are disabled.");
		});
	}

	@Test
	void explicitDisableDoesNotCreateAClientEvenWithAKey() {
		this.runner.withPropertyValues("metadata.ai.create-image-description=false", "spring.ai.openai.api-key=test-key",
				"spring.ai.openai.base-url=not-a-valid-url").run((context) -> {
			assertThat(context).hasNotFailed();
			assertThat(context.getBean(MetadataService.class).getImageDescription(new ByteArrayResource(new byte[0])))
				.isEqualTo("Image descriptions are disabled.");
		});
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void configuredKeyGeneratesAndCachesDescriptions(boolean chatOverrides) throws Exception {
		AtomicInteger requests = new AtomicInteger();
		AtomicReference<String> authorization = new AtomicReference<>();
		AtomicReference<String> requestBody = new AtomicReference<>();
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/v1/chat/completions", (exchange) -> {
			requests.incrementAndGet();
			authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
			requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			byte[] response = """
					{"id":"chatcmpl-test","object":"chat.completion","created":123,"model":"test-model",
					"choices":[{"index":0,"message":{"role":"assistant","content":"A test landscape."},
					"finish_reason":"stop"}],"usage":{"prompt_tokens":1,"completion_tokens":1,"total_tokens":2}}
					""".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, response.length);
			try (var output = exchange.getResponseBody()) {
				output.write(response);
			}
		});
		server.start();
		try {
			Path image = this.directory.resolve("photo.jpg");
			Files.write(image, new ClassPathResource("test-image.jpg").getContentAsByteArray());
			String prefix = chatOverrides ? "spring.ai.openai.chat." : "spring.ai.openai.";
			String modelProperty = chatOverrides ? "spring.ai.openai.chat.model" : "spring.ai.openai.chat.options.model";
			this.runner.withPropertyValues(prefix + "api-key=test-key",
					prefix + "base-url=http://127.0.0.1:" + server.getAddress().getPort() + "/v1",
					modelProperty + "=test-model").run((context) -> {
				assertThat(context).hasNotFailed();
				MetadataService service = context.getBean(MetadataService.class);
				assertThat(service.getImageDescription(new FileSystemResource(image))).isEqualTo("A test landscape.");
				assertThat(service.getImageDescription(new FileSystemResource(image))).isEqualTo("A test landscape.");
				assertThat(Files.readString(this.directory.resolve("photo.txt"))).isEqualTo("A test landscape.");
				assertThat(requests).hasValue(1);
				assertThat(authorization.get()).isEqualTo("Bearer test-key");
				assertThat(requestBody.get()).contains("test-model", "Describe this image.", "data:image/jpeg;base64,");
			});
		}
		finally {
			server.stop(0);
		}
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties({ MetadataConfigProperties.class, OpenAiConfigProperties.class })
	@Import(DefaultMetadataService.class)
	static class TestConfiguration {
	}

}
