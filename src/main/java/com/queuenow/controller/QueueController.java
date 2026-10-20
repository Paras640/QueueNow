package com.queuenow.controller;

import com.queuenow.model.QueueEntry;
import com.queuenow.service.QueueService;
import com.queuenow.service.QRCodeService;
import com.google.zxing.WriterException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * REST controller for customer queue management and queue-related QR codes.
 *
 * <p>Provides endpoints to:
 * <ul>
 *   <li>Join the queue with customer validation ({@code POST /api/queue/join})</li>
 *   <li>Retrieve current list of queue entries ({@code GET /api/queue})</li>
 *   <li>Generate dynamic QR codes for queue access or tokens ({@code GET /api/queue/qr})</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/queue")
public class QueueController {

    /** Service handling queue data structures and token increments. */
    private final QueueService queueService;

    /** Service handling QR code barcode generation. */
    private final QRCodeService qrCodeService;

    /**
     * Constructs a new QueueController injecting queue and QR services.
     *
     * @param queueService  the service handling queue operations
     * @param qrCodeService the service generating QR code images
     */
    public QueueController(
            QueueService queueService,
            QRCodeService qrCodeService) {
        this.queueService = queueService;
        this.qrCodeService = qrCodeService;
    }

    /**
     * Allows a customer to join the waiting queue by providing their name.
     *
     * <p>Endpoint: {@code POST /api/queue/join}</p>
     *
     * @param request JSON payload containing the customer's name, e.g. {@code {"name": "Alice"}}
     * @return {@link ResponseEntity} with token and confirmation if successful, or 400 Bad Request on invalid input
     */
    @PostMapping("/join")
    public ResponseEntity<?> joinQueue(
            @RequestBody Map<String, String> request) {

        // Check if the request body payload is entirely missing
        if (request == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Request cannot be empty."));
        }

        // Extract the customer name from the JSON map
        String name = request.get("name");

        // Default name if omitted for instant QR scanning
        if (name == null || name.trim().isEmpty()) {
            name = "Customer";
        }

        // Add the customer to the queue and obtain the assigned token entry
        QueueEntry entry = queueService.joinQueue(name.trim());

        // Return HTTP 200 OK with confirmation details and assigned token number
        return ResponseEntity.ok(
                Map.of(
                        "message", "You have joined the queue.",
                        "tokenNumber", entry.getTokenNumber(),
                        "customerName", entry.getCustomerName()
                )
        );
    }

    /**
     * Retrieves all active customers in the queue in arrival order.
     *
     * <p>Endpoint: {@code GET /api/queue}</p>
     *
     * @return a list of all current {@link QueueEntry} objects
     */
    @GetMapping
    public List<QueueEntry> getQueue() {
        return queueService.getQueue();
    }

    /**
     * Retrieves the current serving token and queue status for live tracking.
     *
     * <p>Endpoint: {@code GET /api/queue/status}</p>
     */
    @GetMapping("/status")
    public Map<String, Object> getQueueStatus() {
        List<QueueEntry> all = queueService.getQueue();
        return Map.of(
                "currentServing", queueService.getCurrentServingToken(),
                "totalInQueue", all.size(),
                "hasWaiting", queueService.hasWaitingCustomers(),
                "isServing", queueService.isCurrentlyServing(),
                "queue", all
        );
    }

    /**
     * Advances to the next customer token (called by counter / admin).
     *
     * <p>Endpoint: {@code POST /api/queue/next}</p>
     */
    @PostMapping("/next")
    public Map<String, Object> callNext() {
        int next = queueService.callNextToken();
        return Map.of(
                "message", next > 0 ? "Now serving token #" + next : "Queue completed. No waiting customers.",
                "currentServing", next,
                "hasWaiting", queueService.hasWaitingCustomers(),
                "isServing", queueService.isCurrentlyServing()
        );
    }

    /**
     * Concludes the current serving customer's turn, triggering their feedback form,
     * and sets the counter to idle (finish queue toggle).
     *
     * <p>Endpoint: {@code POST /api/queue/finish}</p>
     */
    @PostMapping("/finish")
    public Map<String, Object> finishCurrent() {
        boolean finished = queueService.finishCurrentToken();
        return Map.of(
                "message", finished ? "Turn finished. Customer prompted for feedback." : "No active customer being served.",
                "currentServing", 0,
                "hasWaiting", queueService.hasWaitingCustomers(),
                "isServing", false
        );
    }

    /**
     * Clears all queue tokens and resets token numbering back to 1.
     *
     * <p>Endpoint: {@code POST /api/queue/reset}</p>
     */
    @PostMapping("/reset")
    public Map<String, Object> resetQueue() {
        queueService.resetQueue();
        return Map.of(
                "message", "Queue has been reset successfully.",
                "currentServing", 0,
                "totalInQueue", 0,
                "hasWaiting", false,
                "isServing", false
        );
    }

    /**
     * Records customer feedback after service completion.
     *
     * <p>Endpoint: {@code POST /api/queue/feedback}</p>
     */
    @PostMapping("/feedback")
    public ResponseEntity<?> submitFeedback(@RequestBody Map<String, Object> payload) {
        if (payload == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Payload required."));
        }
        int tokenNumber = 0;
        try {
            tokenNumber = Integer.parseInt(String.valueOf(payload.getOrDefault("tokenNumber", "0")));
        } catch (Exception ignored) {}

        String customerName = String.valueOf(payload.getOrDefault("customerName", "Customer"));
        int rating = 5;
        try {
            rating = Integer.parseInt(String.valueOf(payload.getOrDefault("rating", "5")));
        } catch (Exception ignored) {}

        String comments = String.valueOf(payload.getOrDefault("comments", ""));

        Map<String, Object> saved = queueService.recordFeedback(tokenNumber, customerName, rating, comments);
        return ResponseEntity.ok(Map.of("message", "Feedback recorded successfully", "feedback", saved));
    }

    /**
     * Retrieves all submitted feedback.
     *
     * <p>Endpoint: {@code GET /api/queue/feedback}</p>
     */
    @GetMapping("/feedback")
    public List<Map<String, Object>> getFeedback() {
        return queueService.getFeedbackList();
    }

    /**
     * Inspects live MongoDB status and returns collections contents.
     *
     * <p>Endpoint: {@code GET /api/queue/mongo-status}</p>
     */
    @GetMapping("/mongo-status")
    public Map<String, Object> getMongoStatus() {
        return queueService.getMongoStatus();
    }

    /**
     * Generates a QR code image specifically for queue interaction.
     *
     * <p>Endpoint: {@code GET /api/queue/qr?text=...}</p>
     *
     * @param text string content or URL to encode into the QR code
     * @return {@link ResponseEntity} containing the PNG image with cache-control headers, or 400 Bad Request if text is empty
     * @throws WriterException if QR code encoding encounters an error
     * @throws IOException     if image byte streaming fails
     */
    @GetMapping(value = "/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<?> getQRCode(
            @RequestParam(required = false) String text)
            throws WriterException, IOException {

        // Validate that the query parameter 'text' is provided and not empty
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "QR code text cannot be empty."
                    ));
        }

        // Generate the QR code PNG image byte array
        byte[] qrCode = qrCodeService.generate(text.trim());

        // Return HTTP 200 OK with no-store cache control to ensure freshly generated image delivery
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.IMAGE_PNG)
                .body(qrCode);
    }

    /**
     * Retrieves server network info and the exact join URL for mobile scanning.
     *
     * <p>Endpoint: {@code GET /api/queue/server-info}</p>
    /**
     * Retrieves server network info and the exact join URL for mobile scanning.
     * Supports both public cloud/tunnel URLs (for mobile data / remote team) and LAN IP fallback.
     *
     * <p>Endpoint: {@code GET /api/queue/server-info}</p>
     */
    @GetMapping("/server-info")
    public Map<String, Object> getServerInfo(HttpServletRequest request) {
        String base = resolveBaseUrl(request);
        String ip = getLocalIpAddress();
        int port = request.getServerPort();
        return Map.of(
                "ip", ip,
                "port", port,
                "publicUrl", base,
                "joinUrl", base + "/join.html"
        );
    }

    /**
     * Generates a QR code image specifically encoded with the mobile join queue URL.
     * Display this on the laptop/counter screen so mobile devices can scan and join.
     *
     * <p>Endpoint: {@code GET /api/queue/join-qr}</p>
     */
    @GetMapping(value = "/join-qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<?> getJoinQRCode(HttpServletRequest request)
            throws WriterException, IOException {
        String base = resolveBaseUrl(request);
        String joinUrl = base + "/join.html";

        byte[] qrCode = qrCodeService.generate(joinUrl);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.IMAGE_PNG)
                .body(qrCode);
    }

    /**
     * Generates a QR code image specifically encoded with the customer's direct tracking URL.
     * When scanned by any mobile camera, it opens /track.html with their token number and live queue position.
     *
     * <p>Endpoint: {@code GET /api/queue/track-qr?token=...&name=...}</p>
     */
    @GetMapping(value = "/track-qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<?> getTrackQRCode(
            @RequestParam int token,
            @RequestParam(required = false) String name,
            HttpServletRequest request) throws WriterException, IOException {
        String base = resolveBaseUrl(request);
        String trackUrl = base + "/track.html?token=" + token;
        if (name != null && !name.trim().isEmpty()) {
            trackUrl += "&name=" + URLEncoder.encode(name.trim(), StandardCharsets.UTF_8);
        }

        byte[] qrCode = qrCodeService.generate(trackUrl);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.IMAGE_PNG)
                .body(qrCode);
    }

    private String resolveBaseUrl(HttpServletRequest request) {
        // Check PUBLIC_URL environment variable (set during deployment or tunnel)
        String envPublic = System.getenv("PUBLIC_URL");
        if (envPublic != null && !envPublic.trim().isEmpty()) {
            return envPublic.trim().replaceAll("/+$", "");
        }

        // Check standard reverse proxy headers (Cloudflare, Render, Railway, ngrok)
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        if (forwardedHost != null && !forwardedHost.trim().isEmpty()) {
            String proto = request.getHeader("X-Forwarded-Proto");
            if (proto == null || proto.trim().isEmpty()) {
                proto = "https";
            }
            return proto + "://" + forwardedHost.split(",")[0].trim();
        }

        // Check Host header if it is not localhost
        String host = request.getHeader("Host");
        if (host != null && !host.startsWith("localhost") && !host.startsWith("127.0.0.1")) {
            String proto = request.getHeader("X-Forwarded-Proto");
            if (proto == null || proto.trim().isEmpty()) {
                proto = (host.contains("trycloudflare.com") || host.contains("ngrok") || request.isSecure()) ? "https" : "http";
            }
            return proto + "://" + host;
        }

        // Default to local Wi-Fi IP address
        String ip = getLocalIpAddress();
        int port = request.getServerPort();
        return "http://" + ip + ":" + port;
    }

    /**
     * Resolves the primary local network IP address (e.g. 192.168.x.x or 10.x.x.x)
     * so mobile devices on the same Wi-Fi network can connect directly.
     */
    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        String host = addr.getHostAddress();
                        if (host.startsWith("192.168.") || host.startsWith("10.")) {
                            return host;
                        }
                    }
                }
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "localhost";
        }
    }
}
