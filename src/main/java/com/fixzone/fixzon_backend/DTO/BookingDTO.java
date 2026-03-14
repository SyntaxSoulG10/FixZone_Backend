package com.fixzone.fixzon_backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDTO {
    private UUID bookingId;
    private UUID centerId;
    private UUID tenantId;
    private UUID customerId;
    private UUID vehicleId;
    private UUID packageId;
    private LocalDateTime preferredDateTime;
    private UUID assignedMechanicId;
    private String status;
    private String priority;
    private BigDecimal estimatedCost;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
