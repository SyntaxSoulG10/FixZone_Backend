package com.fixzone.fixzon_backend.DTO;

import lombok.Data;

@Data
public class PaymentRequest {
    private Long bookingId;
    private Double amount;
}
