package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.BookingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BookingHistoryRepository extends JpaRepository<BookingHistory, UUID> {
}
