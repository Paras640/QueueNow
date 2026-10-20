// Maven dependencies:
//   nl.martijndwars:web-push:5.1.1
//   org.bouncycastle:bcprov-jdk18on (a recent version)
//   com.fasterxml.jackson.core:jackson-databind (already needed by Javalin's ctx.json)
//
// Generate VAPID keys once (on your laptop):  npx web-push generate-vapid-keys
// Set env vars on the server: VAPID_PUBLIC, VAPID_PRIVATE, VAPID_SUBJECT (e.g. mailto:you@college.edu)

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PushServiceHelper {

    // token -> subscription. In memory is fine for a demo; store in MongoDB if it must survive restarts.
    private static final Map<String, Map<String, Object>> subscriptions = new ConcurrentHashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper();

    private static PushService pushService;
    private static String publicKey;

    public static void init(Javalin app) throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        publicKey = System.getenv("VAPID_PUBLIC");
        String privateKey = System.getenv("VAPID_PRIVATE");
        String subject = System.getenv().getOrDefault("VAPID_SUBJECT", "mailto:admin@example.com");

        if (publicKey == null || privateKey == null) {
            throw new IllegalStateException("VAPID_PUBLIC and VAPID_PRIVATE env vars must be set");
        }
        pushService = new PushService(publicKey, privateKey, subject);

        app.get("/api/push/public-key", ctx -> ctx.json(Map.of("publicKey", publicKey)));

        app.post("/api/push/subscribe", ctx -> {
            Map<?, ?> body = ctx.bodyAsClass(Map.class);
            String token = String.valueOf(body.get("token"));
            @SuppressWarnings("unchecked")
            Map<String, Object> sub = (Map<String, Object>) body.get("subscription");
            subscriptions.put(token, sub);
            System.out.println("[push] subscribed token=" + token + " (total " + subscriptions.size() + ")");
            ctx.status(201).json(Map.of("ok", true));
        });

        // ── TEMPORARY TEST ENDPOINTS (remove before production) ──
        // Send a test push:   /api/push/test/15
        app.get("/api/push/test/{token}", ctx -> {
            String token = ctx.pathParam("token");
            if (!subscriptions.containsKey(token)) {
                ctx.status(404).result("No subscription stored for token " + token
                        + ". Open the tracker on the phone and tap Notify Me again.");
                return;
            }
            String result = sendToToken(token, "Test 🔔", "Push works!");
            ctx.result(result);
        });

        // See which tokens are subscribed:   /api/push/debug
        app.get("/api/push/debug", ctx -> ctx.json(Map.of("subscribedTokens", subscriptions.keySet())));
    }

    /** Convenience overload so you can pass the numeric token directly. */
    public static String sendToToken(int token, String title, String message) {
        return sendToToken(String.valueOf(token), title, message);
    }

    /**
     * Call this when a person's turn is near, e.g.
     * sendToToken(15, "QueueNow 🔔", "You're next! Get ready.");
     * Returns a short status string (handy for the test endpoint and logs).
     */
    @SuppressWarnings("unchecked")
    public static String sendToToken(String token, String title, String message) {
        Map<String, Object> sub = subscriptions.get(token);
        if (sub == null) {
            System.out.println("[push] no subscription for token=" + token);
            return "no subscription for token " + token;
        }

        try {
            Map<String, String> keys = (Map<String, String>) sub.get("keys");

            Map<String, Object> payloadMap = new LinkedHashMap<>();
            payloadMap.put("title", title);
            payloadMap.put("body", message);
            payloadMap.put("data", Map.of("url", "/track.html"));
            String payload = mapper.writeValueAsString(payloadMap); // safely escapes quotes etc.

            Notification n = new Notification(
                    (String) sub.get("endpoint"),
                    keys.get("p256dh"),
                    keys.get("auth"),
                    payload.getBytes(StandardCharsets.UTF_8));

            HttpResponse response = pushService.send(n);
            int status = response.getStatusLine().getStatusCode();
            System.out.println("[push] token=" + token + " -> HTTP " + status);

            if (status == 404 || status == 410) {
                subscriptions.remove(token); // user unsubscribed or subscription expired
                return "subscription expired (HTTP " + status + "), removed";
            }
            if (status >= 200 && status < 300) {
                return "sent (HTTP " + status + ")";
            }
            return "push service rejected it (HTTP " + status + ")";
        } catch (Exception e) {
            e.printStackTrace();
            return "error: " + e.getMessage();
        }
    }
}