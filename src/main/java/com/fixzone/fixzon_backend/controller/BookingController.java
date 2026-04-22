package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.booking.BookingRequestDTO;
import com.fixzone.fixzon_backend.DTO.booking.BookingResponseDTO;
import com.fixzone.fixzon_backend.service.BookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }


    // EXISTING READ-ONLY ENDPOINTS (BACKWARDS COMPACT)


    @GetMapping
    public ResponseEntity<List<BookingResponseDTO>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<BookingResponseDTO> getBookingById(@PathVariable UUID id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }
    
    @GetMapping("/center/{centerId}")
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByCenter(@PathVariable UUID centerId) {
        return ResponseEntity.ok(bookingService.getBookingsByCenter(centerId));
    }
    
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(bookingService.getBookingsByCustomer(customerId));
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(bookingService.getBookingsByStatus(status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBooking(@PathVariable UUID id) {
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mechanic/{mechanicId}")
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByMechanic(
            @PathVariable UUID mechanicId
    ) {
        return ResponseEntity.ok(bookingService.getBookingsByMechanic(mechanicId));
    }

    @GetMapping("/availability")
    public ResponseEntity<Boolean> checkSlotAvailability(
            @RequestParam UUID centerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time
    ) {
        boolean taken = bookingService.isSlotTaken(centerId, date, time);
        return ResponseEntity.ok(!taken);
    }

    @GetMapping("/active")
    public ResponseEntity<List<BookingResponseDTO>> getActiveBookings() {
        return ResponseEntity.ok(bookingService.getBookingsByStatus("CONFIRMED"));
    }


    /**
     * Create a new booking.
     */
    @PostMapping
    public ResponseEntity<BookingResponseDTO> createBooking(@RequestBody BookingRequestDTO request) {
        return ResponseEntity.ok(bookingService.createBooking(request));
    }

    /**
     * Reschedule an existing booking.
     * Rule: Can only be done at least 3 days before the scheduled date.
     */
    @PutMapping("/{id}/reschedule")
    public ResponseEntity<BookingResponseDTO> rescheduleBooking(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime newTime
    ) {
        return ResponseEntity.ok(bookingService.rescheduleBooking(id, newDate, newTime));
    }

    /**
     * Cancel a booking.
     * Rule: Within 3 days incurs a 5% penalty.
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<BookingResponseDTO> cancelBooking(@PathVariable UUID id) {
        return ResponseEntity.ok(bookingService.cancelBooking(id));
    }
    /**
     * Complete payment for a booking.
     * Status transitions from PENDING_PAYMENT to CONFIRMED.
     */
    @PostMapping("/{id}/payment")
    public ResponseEntity<BookingResponseDTO> completePayment(
            @PathVariable UUID id,
            @RequestParam String gatewaySessionId
    ) {
        BookingResponseDTO response = bookingService.completePayment(id, gatewaySessionId);
        return ResponseEntity.ok(response);
    }

    /**
     * Mark the service/service center job as completed.
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<BookingResponseDTO> completeBooking(@PathVariable UUID id) {
        return ResponseEntity.ok(bookingService.completeBooking(id));
    }


    // Legacy endpoints removed
}
