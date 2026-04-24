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
public class InvoiceDTO {
    private UUID invoiceId;
    private String companyCode;

    @NotNull(message = "Center ID is required")
    private UUID centerId;

    @NotNull(message = "Booking ID is required")
    private UUID bookingId;

    @NotNull(message = "Customer ID is required")
    private UUID issuedToCustomerId;

    @NotNull(message = "Subtotal is required")
    private BigDecimal subtotal;

    private BigDecimal tax;
    private BigDecimal discount;

    @NotNull(message = "Total is required")
    private BigDecimal total;

    private String status;
    private LocalDateTime issuedAt;
    private LocalDateTime dueAt;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
