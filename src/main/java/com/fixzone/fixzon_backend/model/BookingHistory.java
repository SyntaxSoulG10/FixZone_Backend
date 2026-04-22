package com.fixzone.fixzon_backend.model;

import com.fixzone.fixzon_backend.enums.BookingAction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "booking_histories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingHistory {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 50)
    private BookingAction action;

    @Column(name = "old_date")
    private LocalDate oldDate;

    @Column(name = "old_time")
    private LocalTime oldTime;

    @Column(name = "new_date")
    private LocalDate newDate;

    @Column(name = "new_time")
    private LocalTime newTime;

    @Column(name = "penalty", precision = 10, scale = 2)
    private BigDecimal penalty;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
