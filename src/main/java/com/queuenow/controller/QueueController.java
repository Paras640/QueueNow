package com.queuenow.controller;

import com.queuenow.model.QueueEntry;
import com.queuenow.service.QueueService;
import com.queuenow.service.QRCodeService;
import com.google.zxing.WriterException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
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

        // Validate that customer name is present and contains non-whitespace characters
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Please enter your name."));
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
        // Fetch and return the list of queue entries from the service layer
        return queueService.getQueue();
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
}
