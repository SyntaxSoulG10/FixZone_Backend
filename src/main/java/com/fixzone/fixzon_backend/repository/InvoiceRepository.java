package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.Invoice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

import java.time.LocalDateTime;
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

    // Monthly revenue for the last N months (PostgreSQL compatible)
    @Query("""
        SELECT EXTRACT(YEAR FROM i.issuedAt), EXTRACT(MONTH FROM i.issuedAt), SUM(i.total)
        FROM Invoice i
        WHERE i.status = 'PAID'
        AND i.issuedAt >= :start
        GROUP BY EXTRACT(YEAR FROM i.issuedAt), EXTRACT(MONTH FROM i.issuedAt)
        ORDER BY EXTRACT(YEAR FROM i.issuedAt), EXTRACT(MONTH FROM i.issuedAt)
        """)
    List<Object[]> findMonthlyRevenueSince(@Param("start") LocalDateTime start);

    // Weekly revenue breakdown (PostgreSQL compatible: 0=Sunday, 1=Monday... 6=Saturday)
    @Query("""
        SELECT EXTRACT(DOW FROM i.issuedAt), SUM(i.total)
        FROM Invoice i
        WHERE i.status = 'PAID'
        AND i.issuedAt >= :start AND i.issuedAt <= :end
        GROUP BY EXTRACT(DOW FROM i.issuedAt)
        ORDER BY EXTRACT(DOW FROM i.issuedAt)
        """)
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
