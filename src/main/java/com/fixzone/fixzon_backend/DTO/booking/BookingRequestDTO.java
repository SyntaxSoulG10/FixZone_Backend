package com.fixzone.fixzon_backend.DTO.booking;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
public class BookingRequestDTO {

    // Service center selected by user
    @NotNull(message = "Service center is required")
    private UUID centerId;

    // Selected service package
    @NotNull(message = "Service package is required")
    private UUID packageId;

    // Vehicle user selected
    @NotNull(message = "Vehicle is required")
    private UUID vehicleId;

    // Booking date (from calendar)
    @NotNull(message = "Booking date is required")
    private LocalDate bookingDate;

    // Time slot (from UI)
    @NotNull(message = "Booking time is required")
    private LocalTime bookingTime;

    // IDs for tracking
    private UUID customerId;
    private UUID tenantId;

    // Optional note (special request)
    private String specialRequest;
}
