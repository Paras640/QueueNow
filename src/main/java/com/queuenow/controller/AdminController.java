package com.queuenow.controller;

import com.queuenow.model.Admin;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller handling admin authentication and password resets.
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final MongoTemplate mongoTemplate;

    /** In-memory fallback credential storage. */
    private final Map<String, Admin> localAdmins = new ConcurrentHashMap<>();

    public AdminController(@Autowired(required = false) MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void initDefaultAdmin() {
        Admin defaultAdmin = new Admin();
        defaultAdmin.setUsername("admin");
        defaultAdmin.setPassword("admin123");
        defaultAdmin.setRole("ADMIN");
        defaultAdmin.setAssignedCounter(1);
        localAdmins.put("admin", defaultAdmin);

        if (mongoTemplate != null) {
            try {
                Query query = new Query(Criteria.where("username").is("admin"));
                Admin existing = mongoTemplate.findOne(query, Admin.class, "admins");
                if (existing == null) {
                    mongoTemplate.save(defaultAdmin, "admins");
                } else {
                    localAdmins.put("admin", existing);
                }
            } catch (Exception ignored) {
            }
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> payload) {
        String username = payload != null ? payload.get("username") : null;
        String password = payload != null ? payload.get("password") : null;

        Map<String, Object> response = new HashMap<>();

        if (username == null || password == null) {
            response.put("success", false);
            response.put("message", "Username and password are required.");
            return ResponseEntity.status(400).body(response);
        }

        String cleanUser = username.trim().toLowerCase();
        Admin admin = findAdmin(cleanUser);

        if (admin != null && admin.getPassword() != null && admin.getPassword().equals(password)) {
            response.put("success", true);
            response.put("token", UUID.randomUUID().toString());
            response.put("message", "Login successful");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Invalid username or password.");
            return ResponseEntity.status(401).body(response);
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody Map<String, String> payload) {
        String username = payload != null ? payload.get("username") : null;
        String newPassword = payload != null ? payload.get("newPassword") : null;

        Map<String, Object> response = new HashMap<>();

        if (username == null || username.trim().isEmpty() || newPassword == null || newPassword.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Username and new password are required.");
            return ResponseEntity.status(400).body(response);
        }

        String cleanUser = username.trim().toLowerCase();
        Admin admin = findAdmin(cleanUser);

        if (admin != null) {
            admin.setPassword(newPassword.trim());
            localAdmins.put(cleanUser, admin);

            if (mongoTemplate != null) {
                try {
                    Query query = new Query(Criteria.where("username").is(cleanUser));
                    Update update = new Update().set("password", newPassword.trim());
                    mongoTemplate.upsert(query, update, "admins");
                } catch (Exception ignored) {
                }
            }

            response.put("success", true);
            response.put("message", "Password reset successfully! You can now log in with your new password.");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Admin user '" + username.trim() + "' not found.");
            return ResponseEntity.status(400).body(response);
        }
    }

    private Admin findAdmin(String cleanUsername) {
        if (mongoTemplate != null) {
            try {
                Query query = new Query(Criteria.where("username").is(cleanUsername));
                Admin admin = mongoTemplate.findOne(query, Admin.class, "admins");
                if (admin != null) {
                    localAdmins.put(cleanUsername, admin);
                    return admin;
                }
            } catch (Exception ignored) {
            }
        }
        return localAdmins.get(cleanUsername);
    }
}
