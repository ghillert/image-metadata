package com.hillert.image.metadata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "metadata")
public class MetadataConfigProperties {

	/**
	 * Do not resize and label returned images.
	 */
	private Boolean doNotResize;

	private Ai ai;

	public Boolean getDoNotResize() {
		return this.doNotResize;
	}

	public void setDoNotResize(Boolean doNotResize) {
		this.doNotResize = doNotResize;
	}

	public Ai getAi() {
		return this.ai;
	}

	public void setAi(Ai ai) {
		this.ai = ai;
	}

	public static class Ai {

		/**
		 * If true, create an image description using AI.
		 */
		private Boolean createImageDescription;

		/**
		 * The AI text prompt to use to generate the image description.
		 */
		private String textPrompt;

		public Boolean getCreateImageDescription() {
			return this.createImageDescription;
		}

		public void setCreateImageDescription(Boolean createImageDescription) {
			this.createImageDescription = createImageDescription;
		}

		public String getTextPrompt() {
			return this.textPrompt;
		}

		public void setTextPrompt(String textPrompt) {
			this.textPrompt = textPrompt;
		}

	}

}
