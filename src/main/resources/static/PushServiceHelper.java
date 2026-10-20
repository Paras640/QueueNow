// Maven dependencies:
//   nl.martijndwars:web-push:5.1.1
//   org.bouncycastle:bcprov-jdk18on (a recent version)
//
// Generate VAPID keys once (on your laptop):  npx web-push generate-vapid-keys
// Set env vars on the server: VAPID_PUBLIC, VAPID_PRIVATE, VAPID_SUBJECT (e.g. mailto:you@college.edu)

import io.javalin.Javalin;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PushServiceHelper {

    // token -> subscription. In memory is fine for a demo; store in MongoDB if it must survive restarts.
    private static final Map<String, Map<String, Object>> subscriptions = new ConcurrentHashMap<>();

    private static PushService pushService;
    private static String publicKey;

    public static void init(Javalin app) throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        publicKey = System.getenv("VAPID_PUBLIC");
        String privateKey = System.getenv("VAPID_PRIVATE");
        String subject = System.getenv().getOrDefault("VAPID_SUBJECT", "mailto:admin@example.com");
        pushService = new PushService(publicKey, privateKey, subject);

        app.get("/api/push/public-key", ctx -> ctx.json(Map.of("publicKey", publicKey)));

        app.post("/api/push/subscribe", ctx -> {
            Map<?, ?> body = ctx.bodyAsClass(Map.class);
            String token = String.valueOf(body.get("token"));
            @SuppressWarnings("unchecked")
            Map<String, Object> sub = (Map<String, Object>) body.get("subscription");
            subscriptions.put(token, sub);
            ctx.status(201).json(Map.of("ok", true));
        });
    }

    /** Call this when a person's turn is near, e.g. sendToToken("A-17", "Your turn is next!"). */
    @SuppressWarnings("unchecked")
    public static void sendToToken(String token, String title, String message) {
        Map<String, Object> sub = subscriptions.get(token);
        if (sub == null) return;

        try {
            Map<String, String> keys = (Map<String, String>) sub.get("keys");
            String payload = "{\"title\":\"" + title + "\",\"body\":\"" + message
                    + "\",\"data\":{\"url\":\"/track.html\"}}";

            Notification n = new Notification(
                    (String) sub.get("endpoint"),
                    keys.get("p256dh"),
                    keys.get("auth"),
                    payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            int status = pushService.send(n).getStatusLine().getStatusCode();
            if (status == 404 || status == 410) {
                subscriptions.remove(token); // user unsubscribed or subscription expired
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}