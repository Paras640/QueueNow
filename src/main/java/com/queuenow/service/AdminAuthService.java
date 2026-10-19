package com.queuenow.service;

import com.queuenow.model.AdminUser;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing admin user authentication, password verification, and recovery key resets.
 */
@Service
public class AdminAuthService {

    private final MongoTemplate mongoTemplate;

    /** In-memory fallback credential storage. */
    private final Map<String, AdminUser> localUsers = new ConcurrentHashMap<>();

    public AdminAuthService(@Autowired(required = false) MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void initDefaultAdmin() {
        // Default administrator credentials:
        // Username: admin
        // Password: admin123
        // Recovery PIN: 9999
        AdminUser defaultAdmin = new AdminUser("admin", "admin123", "9999");
        localUsers.put("admin", defaultAdmin);

        if (mongoTemplate != null) {
            try {
                Query query = new Query(Criteria.where("username").is("admin"));
                AdminUser existing = mongoTemplate.findOne(query, AdminUser.class, "admin_users");
                if (existing == null) {
                    mongoTemplate.save(defaultAdmin, "admin_users");
                } else {
                    localUsers.put("admin", existing);
                }
            } catch (Exception ignored) {}
        }
    }

    /**
     * Authenticates an administrator against stored credentials.
     *
     * @return a generated session token if successful, or null if credentials fail
     */
    public String login(String username, String password) {
        if (username == null || password == null) return null;
        String cleanUser = username.trim().toLowerCase();

        AdminUser user = findUser(cleanUser);
        if (user != null && user.getPassword().equals(password)) {
            return UUID.randomUUID().toString();
        }
        return null;
    }

    /**
     * Resets an administrator's password after verifying their recovery key / PIN.
     *
     * @return true if recovery key matched and password was updated, false otherwise
     */
    public boolean resetPasswordWithRecoveryKey(String username, String recoveryKey, String newPassword) {
        if (username == null || recoveryKey == null || newPassword == null || newPassword.trim().isEmpty()) {
            return false;
        }
        String cleanUser = username.trim().toLowerCase();
        AdminUser user = findUser(cleanUser);

        if (user != null && user.getRecoveryKey() != null && user.getRecoveryKey().equals(recoveryKey.trim())) {
            user.setPassword(newPassword.trim());
            localUsers.put(cleanUser, user);

            if (mongoTemplate != null) {
                try {
                    Query query = new Query(Criteria.where("username").is(cleanUser));
                    Update update = new Update().set("password", newPassword.trim());
                    mongoTemplate.upsert(query, update, "admin_users");
                } catch (Exception ignored) {}
            }
            return true;
        }
        return false;
    }

    private AdminUser findUser(String cleanUsername) {
        if (mongoTemplate != null) {
            try {
                Query query = new Query(Criteria.where("username").is(cleanUsername));
                AdminUser user = mongoTemplate.findOne(query, AdminUser.class, "admin_users");
                if (user != null) {
                    localUsers.put(cleanUsername, user);
                    return user;
                }
            } catch (Exception ignored) {}
        }
        return localUsers.get(cleanUsername);
    }
}
