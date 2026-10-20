// QueueNow Service Worker — Web Push
self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', (event) => event.waitUntil(self.clients.claim()));

// Runs even when the browser/tab is closed (Android), as long as the server sends a push
self.addEventListener('push', (event) => {
    let data = {
        title: 'QueueNow Alert 🔔',
        body: 'Your turn in the queue is approaching!',
        icon: '/favicon.ico',
        badge: '/favicon.ico',
        tag: 'queuenow-alert',
        data: { url: '/track.html' }
    };

    if (event.data) {
        try {
            data = Object.assign(data, event.data.json());
        } catch (e) {
            data.body = event.data.text();
        }
    }

    event.waitUntil(
        self.registration.showNotification(data.title, {
            body: data.body,
            icon: data.icon,
            badge: data.badge,
            tag: data.tag,
            renotify: true,
            vibrate: [200, 100, 200, 100, 400],
            data: data.data || { url: '/track.html' }
        })
    );
});

// Tap on notification: focus the tracking tab if open, otherwise open it
self.addEventListener('notificationclick', (event) => {
    event.notification.close();
    const targetUrl = (event.notification.data && event.notification.data.url) || '/track.html';

    event.waitUntil(
        self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((list) => {
            for (const client of list) {
                if (client.url.includes(targetUrl) && 'focus' in client) {
                    return client.focus();
                }
            }
            return self.clients.openWindow(targetUrl);
        })
    );
});

// Browser rotated the push subscription: tell the page so it can re-send it to the server
self.addEventListener('pushsubscriptionchange', (event) => {
    event.waitUntil(
        self.clients.matchAll({ includeUncontrolled: true }).then((list) => {
            list.forEach((c) => c.postMessage({ type: 'RESUBSCRIBE' }));
        })
    );
});

// Page-triggered notification (only works while the browser is still running)
self.addEventListener('message', (event) => {
    if (event.data && event.data.type === 'SHOW_NOTIFICATION') {
        const p = event.data.payload || {};
        self.registration.showNotification(p.title || 'QueueNow Alert 🔔', {
            body: p.body || 'Your queue status has updated.',
            icon: p.icon || '/favicon.ico',
            badge: p.badge || '/favicon.ico',
            tag: p.tag || 'queuenow-message',
            renotify: true,
            vibrate: [200, 100, 200, 100, 400],
            data: p.data || { url: '/track.html' }
        });
    }
});