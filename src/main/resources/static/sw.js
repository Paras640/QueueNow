// QueueNow Service Worker for Web Push & Real-Time Alerts
const CACHE_NAME = 'queuenow-v1';

self.addEventListener('install', (event) => {
    self.skipWaiting();
});

self.addEventListener('activate', (event) => {
    event.waitUntil(self.clients.claim());
});

// Handle incoming Web Push notifications
self.addEventListener('push', (event) => {
    let data = {
        title: "QueueNow Alert 🔔",
        body: "Your turn in the queue is approaching!",
        icon: "/favicon.ico",
        badge: "/favicon.ico",
        tag: "queuenow-alert",
        data: { url: "/track.html" }
    };

    if (event.data) {
        try {
            const parsed = event.data.json();
            data = Object.assign(data, parsed);
        } catch (e) {
            data.body = event.data.text();
        }
    }

    const options = {
        body: data.body,
        icon: data.icon || "/favicon.ico",
        badge: data.badge || "/favicon.ico",
        tag: data.tag || "queuenow-alert",
        renotify: true,
        vibrate: [200, 100, 200, 100, 400],
        data: data.data || { url: "/track.html" }
    };

    event.waitUntil(
        self.registration.showNotification(data.title, options)
    );
});

// Handle notification click: focus or open tracking tab
self.addEventListener('notificationclick', (event) => {
    event.notification.close();
    const targetUrl = (event.notification.data && event.notification.data.url) ? event.notification.data.url : '/track.html';

    event.waitUntil(
        clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
            for (const client of clientList) {
                if (client.url.includes('/track.html') && 'focus' in client) {
                    return client.focus();
                }
            }
            if (clients.openWindow) {
                return clients.openWindow(targetUrl);
            }
        })
    );
});

// Allow client script to trigger service-worker notifications directly (ensures background delivery)
self.addEventListener('message', (event) => {
    if (event.data && event.data.type === 'SHOW_NOTIFICATION') {
        const payload = event.data.payload || {};
        self.registration.showNotification(payload.title || 'QueueNow Alert 🔔', {
            body: payload.body || 'Your queue status has updated.',
            icon: payload.icon || '/favicon.ico',
            badge: payload.badge || '/favicon.ico',
            tag: payload.tag || 'queuenow-message',
            renotify: true,
            vibrate: [200, 100, 200, 100, 400],
            data: payload.data || { url: '/track.html' }
        });
    }
});
