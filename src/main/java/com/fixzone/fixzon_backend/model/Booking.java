package com.fixzone.fixzon_backend.model;

import com.fixzone.fixzon_backend.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    // =========================
    // 🔑 PRIMARY KEY
    // =========================
    @Id
    @Column(name = "booking_id")
    private UUID bookingId;

    // =========================
    // 🏢 MULTI-TENANT FIELDS
    // =========================
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "center_id", nullable = false)
    private UUID centerId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "package_id")
    private UUID packageId;

    // =========================
    // 📅 BOOKING SCHEDULE
    // =========================
    @Column(name = "booking_date")
    private LocalDate bookingDate;

    @Column(name = "booking_time")
    private LocalTime bookingTime;

    @Column(name = "special_request", length = 1000)
    private String specialRequest;

    // =========================
    // 🔄 STATUS MANAGEMENT
    // =========================
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private BookingStatus status;

    @Column(name = "is_cancelled")
    private Boolean isCancelled = false;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // =========================
    // 💰 PRICING & PAYMENT
    // =========================
    @Column(name = "estimated_cost", precision = 10, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "booking_fee", precision = 10, scale = 2)
    private BigDecimal bookingFee;

    @Column(name = "cancellation_penalty", precision = 10, scale = 2)
    private BigDecimal cancellationPenalty;

    @Column(name = "is_paid")
    private Boolean isPaid = false;

    @Column(name = "payment_id")
    private UUID paymentId;

    // =========================
    // 🔁 RESCHEDULE CONTROL
    // =========================
    @Column(name = "reschedule_count")
    private Integer rescheduleCount = 0;

    // =========================
    // 🛠 OPERATIONS
    // =========================
    @Column(name = "assigned_mechanic_id")
    private UUID assignedMechanicId;

    // =========================
    // 📊 AUDIT FIELDS
    // =========================
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    // =========================
    // ⚙️ AUTO METHODS
    // =========================
    @PrePersist
    protected void onCreate() {
        if (bookingId == null) {
            bookingId = UUID.randomUUID();
        }
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (createdBy == null) {
            createdBy = "SYSTEM"; //Later (when auth is ready), this changes
        }
        if (updatedBy == null) {
            updatedBy = "SYSTEM"; //Later (when auth is ready), this changes
        }
        if (status == null) {
            status = BookingStatus.PENDING_PAYMENT;
        }
        if (isPaid == null) {
            isPaid = false;
        }
        if (rescheduleCount == null) {
            rescheduleCount = 0;
        }
        if (isCancelled == null) {
            isCancelled = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();

        if (updatedBy == null) {
            updatedBy = "SYSTEM"; //Later (when auth is ready), this changes
        }
    }
}
