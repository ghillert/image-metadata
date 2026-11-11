@param int status = 500
@param String error = "Something went wrong"
@template.layout.page(title = "Error", content = @`
<div class="empty py-6">
<div class="empty-header">${status}</div>
<h1 class="empty-title">${error}</h1>
<p class="empty-subtitle text-secondary">We couldn’t complete that request. Return to your images to continue.</p>
<a class="btn btn-primary mt-3" href="/">Back to your images</a>
</div>
`)
