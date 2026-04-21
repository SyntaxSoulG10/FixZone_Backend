package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.enums.BookingStatus;
import com.fixzone.fixzon_backend.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    
    List<Booking> findByCenterId(UUID centerId);
    
    List<Booking> findByCustomerId(UUID customerId);
    
    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByAssignedMechanicId(UUID mechanicId);

    // For user booking history/tabs (pending / completed / active)
    List<Booking> findByCustomerIdAndStatus(UUID customerId, BookingStatus status);

    // 🔥 MOST IMPORTANT: For soft lock + preventing double booking
    @Query("""
    SELECT COUNT(b) > 0 FROM Booking b
    WHERE b.centerId = :centerId
    AND b.bookingDate = :date
    AND b.bookingTime = :time
    AND (
        b.status = 'CONFIRMED'
        OR (b.status = 'PENDING_PAYMENT' AND b.expiresAt > :now)
    )
    """)
    boolean existsActiveSlot(
            @Param("centerId") UUID centerId,
            @Param("date") LocalDate date,
            @Param("time") LocalTime time,
            @Param("now") LocalDateTime now
    );
}
