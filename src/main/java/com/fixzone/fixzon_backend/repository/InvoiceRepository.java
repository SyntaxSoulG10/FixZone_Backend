package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.Invoice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
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

    // Methods needed by AnalyticsService & ServiceCenterService
    List<Invoice> findByCompanyCodeAndIssuedAtBetween(String companyCode, LocalDateTime start, LocalDateTime end);
    List<Invoice> findByCenterIdInAndIssuedAtBetween(Collection<UUID> centerIds, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT SUM(i.total) FROM Invoice i WHERE i.centerId = :centerId AND i.status = 'PAID'")
    BigDecimal sumTotalByCenterId(@Param("centerId") UUID centerId);

    @Query("SELECT i.centerId, SUM(i.total) FROM Invoice i WHERE i.centerId IN :centerIds AND i.status = 'PAID' GROUP BY i.centerId")
    List<Object[]> sumTotalByCenterIdIn(@Param("centerIds") Collection<UUID> centerIds);

    // Monthly revenue for the last N months (PostgreSQL native)
    @Query(value = """
        SELECT EXTRACT(YEAR FROM issued_at) as year, EXTRACT(MONTH FROM issued_at) as month, SUM(total) as total
        FROM invoices
        WHERE status = 'PAID'
        AND issued_at >= :start
        GROUP BY EXTRACT(YEAR FROM issued_at), EXTRACT(MONTH FROM issued_at)
        ORDER BY 1, 2
        """, nativeQuery = true)
    List<Object[]> findMonthlyRevenueSince(@Param("start") LocalDateTime start);

    // Weekly revenue breakdown (PostgreSQL native)
    @Query(value = """
        SELECT EXTRACT(DOW FROM issued_at) as dow, SUM(total) as daily_total
        FROM invoices
        WHERE status = 'PAID'
        AND issued_at >= :start AND issued_at <= :end
        GROUP BY EXTRACT(DOW FROM issued_at)
        ORDER BY 1
        """, nativeQuery = true)
    List<Object[]> findDailyRevenueBetween(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    @Query("SELECT SUM(i.total) FROM Invoice i WHERE i.status = 'PAID' AND i.issuedAt >= :start AND i.issuedAt <= :end")
    BigDecimal sumRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT i.centerId, SUM(i.total) FROM Invoice i WHERE i.status = 'PAID' GROUP BY i.centerId ORDER BY SUM(i.total) DESC")
    List<Object[]> findTopCentersByRevenue(Pageable pageable);

    @Query("SELECT SUM(i.total) FROM Invoice i WHERE i.status = 'PAID'")
    BigDecimal sumTotalRevenue();
}
