package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.enums.BookingStatus;
import com.fixzone.fixzon_backend.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    
    List<Booking> findByCenterId(UUID centerId);
    
    List<Booking> findByCustomerId(UUID customerId);
    
    List<Booking> findByAssignedMechanicId(UUID assignedMechanicId);
    
    List<Booking> findByStatus(BookingStatus status);
    
    List<Booking> findByTenantId(UUID tenantId);

    // For user booking history/tabs
    List<Booking> findByCustomerIdAndStatus(UUID customerId, BookingStatus status);

    // 🔥 MOST IMPORTANT: For soft lock + preventing double booking
    boolean existsByCenterIdAndBookingDateAndBookingTimeAndStatusIn(
        UUID centerId,
        LocalDate bookingDate,
        LocalTime bookingTime,
        List<BookingStatus> statuses
    );
}
