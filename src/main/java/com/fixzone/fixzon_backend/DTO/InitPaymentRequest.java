package com.fixzone.fixzon_backend.DTO;

import lombok.Data;

@Data
public class InitPaymentRequest {
    private String servicePackageId;
    private String vehicleId;
    private String date;
    private String timeSlot;
    private String centerId;
    private String specialRequest;
    private String bookingId; // Added to link to real bookings
}
