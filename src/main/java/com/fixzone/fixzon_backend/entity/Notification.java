package com.fixzone.fixzon_backend.entity;

import com.fixzone.fixzon_backend.model.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    private String type; // INFO, WARNING, SUCCESS
    private boolean isRead;
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "recipient_user_id")
    private User recipient;

    public Notification() {
        this.id = UUID.randomUUID();
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public User getRecipient() { return recipient; }
    public void setRecipient(User recipient) { this.recipient = recipient; }
}
