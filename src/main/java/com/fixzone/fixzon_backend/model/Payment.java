package com.fixzone.fixzon_backend.model;

import com.fixzone.fixzon_backend.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = true)
    private Long bookingId;

    @Column(name = "service_package_id")
    private java.util.UUID servicePackageId;

    @Column(name = "vehicle_id")
    private java.util.UUID vehicleId;

    @Column(name = "center_id")
    private java.util.UUID centerId;

    @Column(name = "tenant_id")
    private java.util.UUID tenantId;

    @Column(name = "date")
    private String date;

    @Column(name = "time_slot")
    private String timeSlot;

    @Column(name = "gateway_session_id", nullable = true)
    private String gatewaySessionId;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
