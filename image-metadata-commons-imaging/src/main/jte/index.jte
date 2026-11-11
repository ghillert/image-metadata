@import java.util.List
@import java.nio.charset.StandardCharsets
@import org.springframework.web.util.UriUtils
@import com.hillert.image.metadata.controller.form.ImageUploadForm
@param ImageUploadForm imageUploadForm
@param long uploadSizeLimit
@param List<String> files
@param String success = null
@param String error = null
@param List<String> validationErrors = List.of()
@template.layout.page(title = "Your images", content = @`
<div class="page-header mb-4">
<div class="page-pretitle">IMAGE WORKSPACE</div>
<h1 class="page-title mb-2">Every image has a story.</h1>
<p class="text-secondary">Upload a photo to explore its metadata, location, and hidden details.</p>
</div>
@if(success != null)
<div class="alert alert-success" role="status">${success}</div>
@endif
@if(error != null)
<div class="alert alert-danger" role="alert">${error}</div>
@endif
@for(var validationError : validationErrors)
<div class="alert alert-danger" role="alert">${validationError}</div>
@endfor
<div class="row g-4">
<div class="col-lg-5">
<section class="card">
<div class="card-header">
<h2 class="card-title">Upload an image</h2>
</div>
<div class="card-body">
<form id="imageForm" action="/" method="post" enctype="multipart/form-data">
<div class="upload-zone mb-4">
<div class="h1 text-primary mb-2" aria-hidden="true">↑</div>
<label class="form-label h3" for="imageFile">Choose your next image</label>
<p class="text-secondary">JPG, PNG or GIF · Up to ${uploadSizeLimit} MB</p>
<input class="form-control" type="file" id="imageFile" name="imageFile" accept="image/jpeg,image/png,image/gif" required>
<div id="file-selection" class="form-hint mt-2" aria-live="polite">No image selected</div>
</div>
<div class="mb-3">
<label for="title" class="form-label">Caption / title <span class="text-secondary fw-normal">(optional)
</span>
</label>
<input class="form-control" id="title" name="title" value="${imageUploadForm.getTitle()}" placeholder="Give this moment a name">
</div>
<div class="mb-4">
<label for="referenceId" class="form-label">Reference ID <span class="text-secondary fw-normal">(optional)
</span>
</label>
<input class="form-control" id="referenceId" name="referenceId" value="${imageUploadForm.getReferenceId()}" placeholder="Your collection or project reference">
</div>
<label class="form-check form-switch mb-3">
<input class="form-check-input" type="checkbox" name="removeMetadata" value="true" checked="${Boolean.TRUE.equals(imageUploadForm.getRemoveMetadata())}">
<span class="form-check-label">Remove existing metadata</span>
</label>
<label class="form-check form-switch mb-4">
<input class="form-check-input" type="checkbox" name="populateWindowsTags" value="true" checked="${Boolean.TRUE.equals(imageUploadForm.getPopulateWindowsTags())}">
<span class="form-check-label">Populate Windows tags</span>
</label>
<button class="btn btn-primary w-100" type="submit">Upload &amp; inspect image</button>
</form>
</div>
</section>
<div class="card mt-3">
<div class="card-body">
<h3 class="card-title">Look beyond the pixels</h3>
<p class="text-secondary mb-2">Explore EXIF camera settings, IPTC information, XMP properties, and GPS coordinates.</p>
<a href="https://github.com/ghillert/image-metadata">About this project ↗</a>
</div>
</div>
</div>
<div class="col-lg-7">
<div class="d-flex justify-content-between align-items-center mb-3">
<h2 class="mb-0">Your images</h2>
<span class="badge bg-blue-lt">${files.size()} ${files.size() == 1 ? "image" : "images"}</span>
</div>
@if(files.isEmpty())
<div class="card">
<div class="empty py-6">
<div class="empty-header text-primary" aria-hidden="true">▧</div>
<p class="empty-title">A fresh collection starts here</p>
<p class="empty-subtitle text-secondary">Upload your first image to reveal the details behind it.</p>
<a href="#imageFile" class="btn btn-outline-primary mt-3">Choose an image</a>
</div>
</div>
@else
<div class="row g-3">
@for(String file : files)
!{String encoded = UriUtils.encodePathSegment(file, StandardCharsets.UTF_8);}
<div class="col-sm-6">
<article class="card h-100">
<a href="${"/image-details/" + encoded}" aria-label="${"Inspect " + file}">
<img class="image-preview card-img-top" src="${"/images/" + encoded + "?width=400&addLabel=false"}" alt="${file}" loading="lazy">
</a>
<div class="card-body">
<h3 class="card-title filename">${file}</h3>
<a class="btn btn-primary w-100 mb-2" href="${"/image-details/" + encoded}">Inspect metadata</a>
<div class="d-flex gap-2">
<a class="btn btn-outline-secondary flex-fill" href="${"/images/" + encoded + "?download=true"}">Download</a>
<a class="btn btn-outline-danger" href="${"/delete-image/" + encoded}" data-confirm-delete>Delete</a>
</div>
</div>
</article>
</div>
@endfor
</div>
@endif
</div>
</div>
`)
