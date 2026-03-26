const CACHE_NAME = "xyron-shell-v1";
const APP_SHELL = [
    "/",
    "/chat.css",
    "/chat.js",
    "/mobile-config.js",
    "/manifest.webmanifest",
    "/app-icon.svg",
    "/favicon.svg",
    "/offline.html"
];

self.addEventListener("install", (event) => {
    event.waitUntil(
        caches.open(CACHE_NAME).then((cache) => cache.addAll(APP_SHELL))
    );
    self.skipWaiting();
});

self.addEventListener("activate", (event) => {
    event.waitUntil(
        caches.keys().then((cacheNames) =>
            Promise.all(
                cacheNames
                    .filter((cacheName) => cacheName !== CACHE_NAME)
                    .map((cacheName) => caches.delete(cacheName))
            )
        )
    );
    self.clients.claim();
});

self.addEventListener("fetch", (event) => {
    if (event.request.method !== "GET") {
        return;
    }

    event.respondWith(
        fetch(event.request).catch(async () => {
            const cachedResponse = await caches.match(event.request);
            if (cachedResponse) {
                return cachedResponse;
            }

            if (event.request.mode === "navigate") {
                return caches.match("/offline.html");
            }

            throw new Error("Network unavailable");
        })
    );
});
