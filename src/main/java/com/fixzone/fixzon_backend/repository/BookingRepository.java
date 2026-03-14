package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    List<Booking> findByCenterId(UUID centerId);
    List<Booking> findByCustomerId(UUID customerId);
    List<Booking> findByAssignedMechanicId(UUID assignedMechanicId);
    List<Booking> findByStatus(String status);
    List<Booking> findByTenantId(UUID tenantId);
}
