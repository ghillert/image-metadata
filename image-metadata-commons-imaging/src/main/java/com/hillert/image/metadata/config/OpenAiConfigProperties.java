package com.hillert.image.metadata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Connection settings for the optional image-description client.
 *
 * @param apiKey the common OpenAI API key
 * @param baseUrl the common OpenAI endpoint
 * @param chat chat-specific overrides
 * @author Gunnar Hillert
 */
@ConfigurationProperties(prefix = "spring.ai.openai")
public record OpenAiConfigProperties(String apiKey, String baseUrl, Chat chat) {

	public String effectiveApiKey() {
		return (this.chat != null && StringUtils.hasText(this.chat.apiKey())) ? this.chat.apiKey() : this.apiKey;
	}

	public String effectiveBaseUrl() {
		return (this.chat != null && StringUtils.hasText(this.chat.baseUrl())) ? this.chat.baseUrl() : this.baseUrl;
	}

	public String effectiveModel() {
		if (this.chat == null) {
			return null;
		}
		if (StringUtils.hasText(this.chat.model())) {
			return this.chat.model();
		}
		return (this.chat.options() != null) ? this.chat.options().model() : null;
	}

	public record Chat(String apiKey, String baseUrl, String model, Options options) {
	}

	public record Options(String model) {
	}

}
