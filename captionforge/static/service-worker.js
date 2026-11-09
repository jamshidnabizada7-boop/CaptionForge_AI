const CACHE='captionforge-shell-v1';
const SHELL=['/','/static/index.html','/static/styles.css','/static/app.js','/static/manifest.json','/static/icon-192.png','/static/icon-512.png'];
self.addEventListener('install',event=>event.waitUntil(caches.open(CACHE).then(cache=>cache.addAll(SHELL))));
self.addEventListener('activate',event=>event.waitUntil(self.clients.claim()));
self.addEventListener('fetch',event=>{const url=new URL(event.request.url);if(url.pathname.startsWith('/api/'))return;event.respondWith(fetch(event.request).catch(()=>caches.match(event.request)))});
