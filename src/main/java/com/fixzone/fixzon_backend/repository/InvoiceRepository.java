package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    List<Invoice> findByCenterId(UUID centerId);
    Optional<Invoice> findByBookingId(UUID bookingId);
    List<Invoice> findByIssuedToCustomerId(UUID customerId);
    List<Invoice> findByStatus(String status);
    List<Invoice> findByCompanyCode(String companyCode);
    List<Invoice> findByCompanyCodeAndIssuedAtBetween(String companyCode, java.time.LocalDateTime start, java.time.LocalDateTime end);
    List<Invoice> findByCenterIdInAndIssuedAtBetween(java.util.Collection<UUID> centerIds, java.time.LocalDateTime start, java.time.LocalDateTime end);
    
    @org.springframework.data.jpa.repository.Query("SELECT SUM(i.total) FROM Invoice i WHERE i.centerId = :centerId AND i.status = 'PAID'")
    java.math.BigDecimal sumTotalByCenterId(@org.springframework.data.repository.query.Param("centerId") UUID centerId);

    @org.springframework.data.jpa.repository.Query("SELECT i.centerId, SUM(i.total) FROM Invoice i WHERE i.centerId IN :centerIds AND i.status = 'PAID' GROUP BY i.centerId")
    List<Object[]> sumTotalByCenterIdIn(@org.springframework.data.repository.query.Param("centerIds") java.util.Collection<UUID> centerIds);
}
