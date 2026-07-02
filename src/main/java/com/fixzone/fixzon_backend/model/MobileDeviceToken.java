package com.fixzone.fixzon_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stores push notification tokens for mobile devices (Expo Push Tokens).
 */
@Entity
@Table(name = "mobile_device_tokens")
public class MobileDeviceToken {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String token; // The Expo Push Token

    private String platform;
    private String deviceName;

    @Column(nullable = false)
    private boolean isActive = true;

    private LocalDateTime lastSeen;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public MobileDeviceToken() {
        this.id = UUID.randomUUID();
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        this.lastSeen = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastSeen = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }
    
    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
}
