package com.fixzone.fixzon_backend.DTO;

import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "Invoice ID is required")
    private UUID invoiceId;

    @NotNull(message = "Center ID is required")
    private UUID centerId;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotNull(message = "Payment method is required")
    private String method;

    private String providerTransactionId;
    private String status;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
