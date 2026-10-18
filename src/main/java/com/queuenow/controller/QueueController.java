
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

@RestController
@RequestMapping("/api/queue")
public class QueueController {

    private final QueueService queueService;
    private final QRCodeService qrCodeService;

    public QueueController(
            QueueService queueService,
            QRCodeService qrCodeService) {
        this.queueService = queueService;
        this.qrCodeService = qrCodeService;
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinQueue(
            @RequestBody Map<String, String> request) {

        String name = request.get("name");

        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Please enter your name."));
        }

        QueueEntry entry = queueService.joinQueue(name.trim());

        return ResponseEntity.ok(
                Map.of(
                        "message", "You have joined the queue.",
                        "tokenNumber", entry.getTokenNumber(),
                        "customerName", entry.getCustomerName()
                )
        );
    }

    @GetMapping
    public List<QueueEntry> getQueue() {
        return queueService.getQueue();
    }

    @GetMapping(value = "/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQRCode(
            @RequestParam String text) throws WriterException, IOException {

        byte[] qrCode = qrCodeService.generate(text);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.IMAGE_PNG)
                .body(qrCode);
    }
}
