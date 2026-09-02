package com.fixzone.fixzon_backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitPaymentResponse {
    private Long paymentId;
    private Double amount;
}
