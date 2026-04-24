package com.fixzone.fixzon_backend.DTO.booking;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
public class BookingRequestDTO {

    // Service center selected by user
    private UUID centerId;

    // Selected service package
    private UUID packageId;

    // Vehicle user selected
    private UUID vehicleId;

    // Booking date (from calendar)
    private LocalDate bookingDate;

    // Time slot (from UI)
    private LocalTime bookingTime;

    // IDs for tracking
    private UUID customerId;
    private UUID tenantId;

    // Optional note (special request)
    private String specialRequest;
}
