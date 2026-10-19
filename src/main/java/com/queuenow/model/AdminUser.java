package com.queuenow.model;

/**
 * Data model representing an administrator user with credentials and recovery key.
 */
public class AdminUser {

    private String username;
    private String password;
    private String recoveryKey;

    public AdminUser() {
    }

    public AdminUser(String username, String password, String recoveryKey) {
        this.username = username;
        this.password = password;
        this.recoveryKey = recoveryKey;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRecoveryKey() {
        return recoveryKey;
    }

    public void setRecoveryKey(String recoveryKey) {
        this.recoveryKey = recoveryKey;
    }
}
