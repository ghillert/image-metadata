@import java.util.Map
@import java.util.List
@import java.nio.charset.StandardCharsets
@import org.springframework.web.util.UriUtils
@import com.hillert.image.metadata.model.Directory
@import com.hillert.image.metadata.model.DirectoryType
@import com.hillert.image.metadata.model.GnssInfo
@param String filename
@param Map<DirectoryType, List<Directory>> metadata
@param int metadataCount
@param GnssInfo mapLocation = null
@param String imageDescription = null
@param boolean imageDescriptionUnavailable = false
@param String xmpData = null
!{String encoded = UriUtils.encodePathSegment(filename, StandardCharsets.UTF_8);}
@template.layout.page(title = filename, content = @`
<div class="page-header mb-4">
<a href="/" class="text-secondary">← Your images</a>
<div class="d-flex flex-wrap gap-3 justify-content-between mt-3">
<div>
<div class="page-pretitle">IMAGE INSPECTOR</div>
<h1 class="page-title filename">${filename}</h1>
</div>
<a class="btn btn-primary" href="${"/images/" + encoded + "?download=true"}">Download image</a>
</div>
</div>
<div class="row g-4">
<div class="col-lg-5">
<section class="card mb-4">
<div class="card-body">
<img class="detail-preview" src="${"/images/" + encoded + "?width=800&addLabel=false"}" alt="${filename}">
<h2 class="card-title mt-4">Image description</h2>
@if(imageDescriptionUnavailable)
<div class="alert alert-warning mb-0" role="status">The image description is temporarily unavailable. Your image metadata is still shown below. Try again later or check the AI service configuration.</div>
@else
<p class="text-secondary mb-0">${imageDescription == null || imageDescription.isBlank() ? "No description available." : imageDescription}</p>
@endif
</div>
</section>
<section class="card">
<div class="card-header">
<h2 class="card-title">Location</h2>
</div>
<div class="card-body">
@if(mapLocation == null)
<p class="text-secondary mb-0">This image has no GPS coordinates.</p>
@else
<div class="map-panel mb-3" data-map data-latitude="${mapLocation.getLatitude()}" data-longitude="${mapLocation.getLongitude()}" aria-label="Image location map">
</div>
<dl class="row mb-0">
<dt class="col-5">Latitude</dt>
<dd class="col-7">${mapLocation.getLatitude()}</dd>
<dt class="col-5">Longitude</dt>
<dd class="col-7">${mapLocation.getLongitude()}</dd>
<dt class="col-5">Altitude</dt>
<dd class="col-7">${mapLocation.getElevation()} m</dd>
</dl>
@endif
</div>
</section>
</div>
<div class="col-lg-7">
<div class="d-flex justify-content-between align-items-center mb-3">
<h2 class="mb-0">Metadata</h2>
<span class="badge bg-blue-lt">${metadataCount} properties</span>
</div>
<label class="visually-hidden" for="metadata-search">Search metadata</label>
<input id="metadata-search" class="form-control mb-3" type="search" placeholder="Search properties and values…">
@for(var group : metadata.entrySet())
<section class="card mb-3">
<div class="card-header">
<div>
<h3 class="card-title">${group.getKey().getName()}</h3>
<div class="text-secondary small">${group.getKey().getDescription()}</div>
</div>
</div>
<div class="table-responsive">
<table class="table table-vcenter card-table metadata-table mb-0">
<thead>
<tr>
<th>Property</th>
<th>Value</th>
</tr>
</thead>
<tbody>
@for(var item : group.getValue())
<tr data-metadata-row>
<th scope="row">${item.getPropertyName()}</th>
<td>${item.getPropertyValue()}</td>
</tr>
@endfor
</tbody>
</table>
</div>
</section>
@endfor
@if(xmpData != null && !xmpData.isBlank())
<details class="card">
<summary class="card-header">Raw XMP data</summary>
<div class="card-body">
<pre class="mb-0">
<code class="language-xml">${xmpData}</code>
</pre>
</div>
</details>
@endif
</div>
</div>
`)
