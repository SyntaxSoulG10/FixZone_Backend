package com.fixzone.fixzon_backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "service_packages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServicePackage {

    @Id
    @Column(name = "package_id")
    private UUID packageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id", nullable = false)
    @JsonIgnore
    private ServiceCenter serviceCenter;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "type", columnDefinition = "TEXT")
    private String type; // Can be used for comma-separated features

    /**
     * Restricts this package to a specific vehicle type.
     * Null means compatible with all vehicle types.
     * Expected values: "CAR", "BIKE", "VAN", "TRUCK", null (any)
     */
    @Column(name = "vehicle_type", length = 20)
    private String vehicleType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "base_price", precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "estimated_duration_mins")
    private Integer estimatedDurationMins;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        if (packageId == null) {
            packageId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
