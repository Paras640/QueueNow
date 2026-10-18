
package com.queuenow.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/queue")
public class QueueController {

    @PostMapping("/join")
    public Map<String, String> joinQueue(
            @RequestBody Map<String, String> request) {

        String name = request.get("name");

        if (name == null || name.trim().isEmpty()) {
            return Map.of("message", "Please enter your name.");
        }

        return Map.of(
                "message",
                "Welcome " + name + "! You have joined the queue."
        );
    }
}
