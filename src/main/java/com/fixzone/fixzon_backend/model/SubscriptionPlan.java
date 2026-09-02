package com.fixzone.fixzon_backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "subscription_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlan {
    @Id
    @Column(name = "plan_id")
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer durationMonths;

    @Column(nullable = false)
    private Boolean isActive = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "subscription_plan_features", joinColumns = @JoinColumn(name = "plan_id"))
    @Column(name = "feature")
    private java.util.List<String> features = new java.util.ArrayList<>();

    @Column(name = "is_popular")
    private Boolean isPopular = false;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private java.time.LocalDateTime updatedAt;

    public SubscriptionPlan(UUID id, String name, BigDecimal price, String description, Integer durationMonths, Boolean isActive, java.util.List<String> features, Boolean isPopular) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.description = description;
        this.durationMonths = durationMonths;
        this.isActive = isActive;
        this.features = features;
        this.isPopular = isPopular;
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        this.createdAt = java.time.LocalDateTime.now();
        this.updatedAt = java.time.LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = java.time.LocalDateTime.now();
    }
}
