
package com.queuenow.controller;

import com.queuenow.model.QueueEntry;
import com.queuenow.service.QueueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/queue")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
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
}
