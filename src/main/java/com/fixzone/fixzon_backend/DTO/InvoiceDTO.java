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
public class InvoiceDTO {
    private UUID invoiceId;
    private String companyCode;
    private UUID centerId;
    private UUID bookingId;
    private UUID issuedToCustomerId;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal discount;
    private BigDecimal total;
    private String status;
    private LocalDateTime issuedAt;
    private LocalDateTime dueAt;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
