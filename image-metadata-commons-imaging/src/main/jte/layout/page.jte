@import gg.jte.Content
@param String title
@param Content content
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="description" content="Inspect, edit and explore the metadata inside your images.">
<title>${title} · Image Metadata</title>
<link rel="icon" href="/favicon.ico">
<link rel="stylesheet" href="/assets/app.css">
</head>
<body>
<a class="skip-link" href="#main-content">Skip to content</a>
<div class="page">
<header class="navbar navbar-expand-md bg-white">
<div class="container-xl">
<a href="/" class="navbar-brand gap-2">
<span class="brand-mark" aria-hidden="true">▧</span> Image Metadata</a>
<a class="btn btn-outline-secondary btn-sm" href="https://github.com/ghillert/image-metadata">View source ↗</a>
</div>
</header>
<main class="page-wrapper" id="main-content">
<div class="container-xl pb-5">${content}</div>
</main>
<footer class="footer footer-transparent">
<div class="container-xl d-flex flex-wrap justify-content-between gap-2">
<span>Image Metadata</span>
<span class="text-secondary">Crafted in Hawaiʻi by Gunnar Hillert</span>
</div>
</footer>
</div>
<script src="/assets/app.js" defer>
</script>
</body>
</html>
