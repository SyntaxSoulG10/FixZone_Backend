package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, UUID> {
    List<PaymentRecord> findByInvoiceId(UUID invoiceId);
    List<PaymentRecord> findByCenterId(UUID centerId);
    List<PaymentRecord> findByCenterIdIn(java.util.Collection<UUID> centerIds);
    List<PaymentRecord> findByCenterIdInAndCreatedAtBetween(java.util.Collection<UUID> centerIds, java.time.LocalDateTime start, java.time.LocalDateTime end);
    List<PaymentRecord> findByStatus(String status);
    List<PaymentRecord> findByMethod(String method);
}
