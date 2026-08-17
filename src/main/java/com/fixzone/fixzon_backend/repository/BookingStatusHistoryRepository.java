package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.BookingStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface BookingStatusHistoryRepository extends JpaRepository<BookingStatusHistory, UUID> {
    List<BookingStatusHistory> findByBookingIdOrderByChangedAtAsc(UUID bookingId);
}
