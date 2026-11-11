package com.hillert.image.metadata.view;

import java.util.List;
import java.util.Map;

import com.hillert.image.metadata.controller.form.ImageUploadForm;
import com.hillert.image.metadata.model.Directory;
import com.hillert.image.metadata.model.DirectoryType;
import com.hillert.image.metadata.model.GnssInfo;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateTests {

	private final TemplateEngine engine = TemplateEngine.createPrecompiled(ContentType.Html);

	@Test
	void rendersEmptyWorkspaceAndUploadOptions() {
		String html = render("index.jte", Map.of("imageUploadForm", new ImageUploadForm(), "uploadSizeLimit", 10L,
				"files", List.of()));
		assertThat(html).contains("A fresh collection starts here", "multipart/form-data", "removeMetadata",
				"populateWindowsTags", "/assets/app.js");
	}

	@Test
	void escapesGalleryNamesAndEncodesPaths() {
		String html = render("index.jte", Map.of("imageUploadForm", new ImageUploadForm(), "uploadSizeLimit", 10L,
				"files", List.of("photo #1<script>.jpg")));
		assertThat(html).contains("photo%20%231%3Cscript%3E.jpg").doesNotContain("<script>.jpg");
	}

	@Test
	void retainsRejectedFormValuesAndValidationFeedback() {
		ImageUploadForm form = new ImageUploadForm();
		form.setTitle("<unsafe>");
		form.setRemoveMetadata(true);
		String html = render("index.jte", Map.of("imageUploadForm", form, "uploadSizeLimit", 10L, "files", List.of(),
				"validationErrors", List.of("Choose a supported image")));
		assertThat(html).contains("&lt;unsafe>", "Choose a supported image", "checked");
	}

	@Test
	void rendersGpsAndEscapesMetadataAndXmp() {
		String html = render("imageDetails.jte", Map.of("filename", "photo.jpg", "metadataCount", 1, "metadata",
				Map.of(DirectoryType.EXIF, List.of(new Directory(DirectoryType.EXIF, "Title", "<script>bad</script>"))),
				"mapLocation", new GnssInfo(20, -155), "xmpData", "<xmp>example</xmp>"));
		assertThat(html).contains("data-latitude=\"20.0\"", "data-longitude=\"-155.0\"", "&lt;script&gt;",
				"&lt;xmp&gt;").doesNotContain("<script>bad</script>");
	}

	@Test
	void rendersMissingGpsAndErrorPages() {
		assertThat(render("imageDetails.jte", Map.of("filename", "photo.jpg", "metadataCount", 0, "metadata", Map.of())))
			.contains("no GPS coordinates", "No description available");
		assertThat(render("error.jte", Map.of("status", 404, "error", "Not Found"))).contains("404", "Not Found");
		assertThat(render("uploadError.jte", Map.of("errorMessage", "Too large"))).contains("Too large");
	}

	private String render(String template, Map<String, Object> model) {
		StringOutput output = new StringOutput();
		this.engine.render(template, model, output);
		return output.toString();
	}

}
