import '@tabler/core/dist/css/tabler.min.css';
import '@tabler/core/dist/js/tabler.min.js';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import hljs from 'highlight.js/lib/core';
import xml from 'highlight.js/lib/languages/xml';
import 'highlight.js/styles/github.css';
import './app.css';
hljs.registerLanguage('xml', xml);
document.querySelectorAll('pre code').forEach(el => hljs.highlightElement(el));
const mapElement = document.querySelector('[data-map]');
if (mapElement) {
  const coordinates = [Number(mapElement.dataset.latitude), Number(mapElement.dataset.longitude)];
  const map = L.map(mapElement).setView(coordinates, 10);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors', maxZoom: 19
  }).addTo(map);
  L.circleMarker(coordinates, { radius: 8, color: '#206bc4', fillOpacity: 0.8 }).addTo(map);
}
const fileInput = document.querySelector('#imageFile');
const selection = document.querySelector('#file-selection');
fileInput?.addEventListener('change', () => { selection.textContent = fileInput.files[0]?.name || 'No image selected'; });
document.querySelector('#imageForm')?.addEventListener('submit', event => {
  if (event.target.checkValidity()) {
    const button = event.target.querySelector('button[type="submit"]');
    button.disabled = true; button.textContent = 'Processing image…';
  }
});
document.querySelectorAll('[data-confirm-delete]').forEach(link => link.addEventListener('click', event => {
  if (!window.confirm('Delete this image? This cannot be undone.')) event.preventDefault();
}));
const search = document.querySelector('#metadata-search');
search?.addEventListener('input', () => {
  const term = search.value.toLowerCase();
  document.querySelectorAll('[data-metadata-row]').forEach(row => { row.hidden = !row.textContent.toLowerCase().includes(term); });
});
