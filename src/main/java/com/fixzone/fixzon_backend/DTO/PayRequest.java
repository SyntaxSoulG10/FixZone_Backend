package com.fixzone.fixzon_backend.DTO;

import lombok.Data;

@Data
public class PayRequest {
    private Long paymentId;
    private String method; // CARD or QR
}
