package com.queuenow.controller;

import com.queuenow.service.AdminAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");

        String token = adminAuthService.login(username, password);
        Map<String, Object> response = new HashMap<>();

        if (token != null) {
            response.put("success", true);
            response.put("token", token);
            response.put("message", "Login successful");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Invalid username or password");
            return ResponseEntity.status(401).body(response);
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String recoveryKey = payload.get("recoveryKey");
        String newPassword = payload.get("newPassword");

        boolean reset = adminAuthService.resetPasswordWithRecoveryKey(username, recoveryKey, newPassword);
        Map<String, Object> response = new HashMap<>();

        if (reset) {
            response.put("success", true);
            response.put("message", "Password reset successfully! You can now log in with your new password.");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Invalid username or security recovery key.");
            return ResponseEntity.status(400).body(response);
        }
    }
}
