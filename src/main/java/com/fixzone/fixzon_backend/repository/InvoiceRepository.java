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
}
