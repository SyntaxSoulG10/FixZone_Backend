package com.fixzone.fixzon_backend.scheduler;

import com.fixzone.fixzon_backend.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OverdueBookingScheduler {

    private static final Logger log = LoggerFactory.getLogger(OverdueBookingScheduler.class);

    private final BookingService bookingService;

    public OverdueBookingScheduler(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Scheduled task to check for overdue CONFIRMED bookings whose 30-minute grace period has expired.
     * Runs every 5 minutes (300,000 milliseconds).
     */
    @Scheduled(fixedRate = 300000)
    public void checkAndCancelOverdueBookings() {
        log.info(">>> RUNNING SCHEDULED JOB: Checking overdue confirmed bookings (30-minute grace period check)...");
        try {
            var cancelledList = bookingService.processOverdueBookings();
            if (!cancelledList.isEmpty()) {
                log.info(">>> OVERDUE BOOKINGS JOB COMPLETED: {} booking(s) auto-cancelled.", cancelledList.size());
            } else {
                log.info(">>> OVERDUE BOOKINGS JOB COMPLETED: No overdue confirmed bookings found.");
            }
        } catch (Exception e) {
            log.error(">>> ERROR IN OVERDUE BOOKINGS SCHEDULED JOB", e);
        }
    }
}
