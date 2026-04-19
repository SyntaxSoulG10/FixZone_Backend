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

    // 🔑 Booking ID
    private UUID bookingId;

    // 🔹 Basic references (used by frontend if needed)
    private UUID centerId;
    private UUID vehicleId;
    private UUID packageId;

    // 📅 Booking schedule
    private LocalDate bookingDate;
    private LocalTime bookingTime;

    // 🔄 Status (for UI: pending / active / completed)
    private BookingStatus status;

    // 💰 Pricing
    private BigDecimal estimatedCost;        // full amount
    private BigDecimal bookingFee;           // 10% paid online
    private BigDecimal cancellationPenalty;  // if cancelled late

    // 💳 Payment
    private Boolean isPaid;

    // 📝 Optional user note
    private String specialRequest;

    // 📊 For UI display (like "Booked on")
    private LocalDateTime createdAt;
}
