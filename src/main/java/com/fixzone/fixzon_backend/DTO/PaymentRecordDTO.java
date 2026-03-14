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
public class PaymentRecordDTO {
    private UUID paymentId;
    private UUID invoiceId;
    private UUID centerId;
    private BigDecimal amount;
    private String method;
    private String providerTransactionId;
    private String status;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
