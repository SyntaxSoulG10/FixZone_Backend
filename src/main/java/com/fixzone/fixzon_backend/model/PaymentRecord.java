package com.fixzone.fixzon_backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRecord {

    @Id
    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "center_id", nullable = false)
    private UUID centerId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "method", length = 50)
    private String method;

    @Column(name = "provider_transaction_id", length = 150)
    private String providerTransactionId;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        if (paymentId == null) {
            paymentId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
