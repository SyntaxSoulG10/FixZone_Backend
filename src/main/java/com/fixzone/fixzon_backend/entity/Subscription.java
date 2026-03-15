package com.fixzone.fixzon_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private String planType; // BASIC, PREMIUM, etc.
    private String status;   // ACTIVE, EXPIRED, CANCELLED
    
    @Column(columnDefinition = "TEXT")
    private String billingHistory;

    @OneToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    public Subscription() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
