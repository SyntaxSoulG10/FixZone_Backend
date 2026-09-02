package com.fixzone.fixzon_backend.DTO;

import lombok.Data;

@Data
public class RefundRequest {
    private Long bookingId;
    private double penaltyPercentage;
}
