package com.fixzone.fixzon_backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @Column(name = "subscription_id")
    private UUID id;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private String planType; // BASIC, PREMIUM, etc.
    private String status;   // ACTIVE, EXPIRED, CANCELLED
    
    @Column(columnDefinition = "TEXT")
    private String billingHistory;

    @OneToOne
    @JoinColumn(name = "owner_user_id")
    private User owner;

    public Subscription() {
        this.id = UUID.randomUUID();
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getPlanType() { return planType; }
    public void setPlanType(String planType) { this.planType = planType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getBillingHistory() { return billingHistory; }
    public void setBillingHistory(String billingHistory) { this.billingHistory = billingHistory; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
}
