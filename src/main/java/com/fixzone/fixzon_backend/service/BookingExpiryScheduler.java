package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.enums.BookingStatus;
import com.fixzone.fixzon_backend.model.Booking;
import com.fixzone.fixzon_backend.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job that automatically transitions bookings to EXPIRED status.
 */
@Component
public class BookingExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookingExpiryScheduler.class);

    private final BookingRepository bookingRepository;

    public BookingExpiryScheduler(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    /**
     * Runs every 60 seconds.
     * Rule: Any PENDING_PAYMENT booking whose expiresAt has passed transitions to EXPIRED.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireStaleBookings() {
        LocalDateTime now = LocalDateTime.now();

        // Rule: PENDING_PAYMENT booking time slot 5 minutes passed
        List<Booking> expiredPending = bookingRepository.findExpiredBookings(now);
        if (!expiredPending.isEmpty()) {
            expiredPending.forEach(b -> {
                b.setStatus(BookingStatus.EXPIRED);
                log.info(">>> [Scheduler] 5 MINUTE PAYMENT WINDOW PASSED → bookingId={} expiresAt={}", b.getBookingId(), b.getExpiresAt());
            });
            bookingRepository.saveAll(expiredPending);
            log.info(">>> [Scheduler] Expired {} PENDING_PAYMENT booking(s)", expiredPending.size());
        }
    }
}


