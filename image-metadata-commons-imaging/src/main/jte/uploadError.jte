@param String errorMessage = "The image could not be uploaded. Please try again."
@template.layout.page(title = "Upload failed", content = @`
<div class="empty py-6">
<div class="empty-header text-danger">!</div>
<h1 class="empty-title">Upload failed</h1>
<p class="empty-subtitle text-secondary" role="alert">${errorMessage}</p>
<a class="btn btn-primary mt-3" href="/">Back to your images</a>
</div>
`)
