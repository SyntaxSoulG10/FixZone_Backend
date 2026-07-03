package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.BookingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface BookingHistoryRepository extends JpaRepository<BookingHistory, UUID> {
}
