package com.fixzone.fixzon_backend.DTO.booking;

import com.fixzone.fixzon_backend.enums.BookingStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
public class BookingResponseDTO {

    // Booking ID
    private UUID bookingId;

    // 🔹 Basic references
    private UUID centerId;
    private UUID vehicleId;
    private UUID packageId;
    private UUID customerId;
    private UUID tenantId;

    // Booking schedule
    private LocalDate bookingDate;
    private LocalTime bookingTime;

    // Status
    private BookingStatus status;

    // Pricing
    private BigDecimal estimatedCost;
    private BigDecimal bookingFee;
    private BigDecimal cancellationPenalty;

    // Payment fields (Stripe)
    private String gatewaySessionId;
    private Boolean bookingFeePaid;

    // Smart Locking
    private LocalDateTime expiresAt;

    // Optional user note
    private String specialRequest;

    // Audit
    private LocalDateTime createdAt;

    // UI Helpers
    private String serviceCenterName;
    private String packageName;
}
