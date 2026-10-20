// Include on track.html:  <script src="/push-client.js"></script>
// Add a button:           <button id="notifyBtn">🔔 Notify me when it's my turn</button>
// Then call:              enablePush(tokenNumber)  from the button's click handler

function urlBase64ToUint8Array(base64String) {
    const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
    const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
    const raw = atob(base64);
    return Uint8Array.from([...raw].map((c) => c.charCodeAt(0)));
}

async function subscribeAndSend(token) {
    const reg = await navigator.serviceWorker.register('/sw.js');
    await navigator.serviceWorker.ready;

    const keyRes = await fetch('/api/push/public-key');
    const { publicKey } = await keyRes.json();

    let sub = await reg.pushManager.getSubscription();
    if (!sub) {
        sub = await reg.pushManager.subscribe({
            userVisibleOnly: true,
            applicationServerKey: urlBase64ToUint8Array(publicKey)
        });
    }

    await fetch('/api/push/subscribe', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: token, subscription: sub })
    });
}

// Call this from a button click (browsers ignore permission prompts that fire on page load)
async function enablePush(token) {
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
        alert('Notifications are not supported in this browser. Use Chrome on Android, or add the site to your home screen on iPhone.');
        return false;
    }
    if (!window.isSecureContext) {
        alert('Notifications need HTTPS. Open the deployed https:// link.');
        return false;
    }

    const permission = await Notification.requestPermission();
    if (permission !== 'granted') {
        alert('Notifications are blocked. Allow them in the site settings to get alerts.');
        return false;
    }

    try {
        await subscribeAndSend(token);
        return true;
    } catch (err) {
        console.error('Push setup failed', err);
        alert('Could not enable notifications. Please try again.');
        return false;
    }
}

// If the browser rotated the subscription, send the new one
if ('serviceWorker' in navigator) {
    navigator.serviceWorker.addEventListener('message', (e) => {
        if (e.data && e.data.type === 'RESUBSCRIBE') {
            const token = sessionStorage.getItem('queueToken'); // store your token here when you get it
            if (token) subscribeAndSend(token).catch(console.error);
        }
    });
}