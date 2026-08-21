package com.fixzone.fixzon_backend.DTO.booking;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TodayBookingDTO {
    private UUID bookingId;
    private UUID customerId;
    private String customerName;
    private String vehicleRegistration;
    private String vehicleBrandType;
    private String servicePackageType;
    private LocalDate bookingDate;
    private LocalTime bookingTime;
}
