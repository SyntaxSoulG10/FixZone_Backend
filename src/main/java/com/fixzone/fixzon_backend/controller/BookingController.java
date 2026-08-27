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

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Bookings", description = "Service booking creation, status management, and schedule slot APIs")
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

    /**
     * Returns bookings for the currently authenticated customer.
     * This is the primary endpoint the customer dashboard should use.
     */
    @GetMapping("/my")
    public ResponseEntity<List<BookingResponseDTO>> getMyBookings(
            org.springframework.security.core.Authentication authentication) {
        return ResponseEntity.ok(bookingService.getBookingsForCurrentCustomer(authentication.getName()));
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
            @PathVariable UUID mechanicId) {
        return ResponseEntity.ok(bookingService.getBookingsByMechanic(mechanicId));
    }

    @GetMapping("/availability")
    public ResponseEntity<Boolean> checkSlotAvailability(
            @RequestParam UUID centerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time) {
        boolean taken = bookingService.isSlotTaken(centerId, date, time);
        return ResponseEntity.ok(!taken);
    }

    @GetMapping("/available-slots")
    public ResponseEntity<List<String>> getAvailableSlots(
            @RequestParam UUID centerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID packageId
    ) {
        return ResponseEntity.ok(bookingService.getAvailableStartTimes(centerId, date, packageId));
    }

    @GetMapping("/available-start-times")
    public ResponseEntity<List<String>> getAvailableStartTimes(
            @RequestParam UUID centerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID packageId
    ) {
        return ResponseEntity.ok(bookingService.getAvailableStartTimes(centerId, date, packageId));
    }

    @GetMapping("/active")
    public ResponseEntity<List<BookingResponseDTO>> getActiveBookings() {
        return ResponseEntity.ok(bookingService.getBookingsByStatus("CONFIRMED"));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<BookingResponseDTO>> getUpcomingBookings(
            @RequestParam(required = false) UUID customerId) {
        return ResponseEntity.ok(bookingService.getUpcomingBookings(customerId));
    }

    /**
     * Create a new booking.
     * customerId is resolved from the JWT — the frontend does not need to send it.
     */
    @PostMapping
    public ResponseEntity<BookingResponseDTO> createBooking(
            @jakarta.validation.Valid @RequestBody BookingRequestDTO request,
            org.springframework.security.core.Authentication authentication) {
        return ResponseEntity.status(201).body(bookingService.createBooking(request, authentication));
    }

    /**
     * Reschedule an existing booking.
     * Rule: Can only be done at least 3 days before the scheduled date.
     */
    @PutMapping("/{id}/reschedule")
    public ResponseEntity<BookingResponseDTO> rescheduleBooking(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newDate,
            @RequestParam String newTime) {
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
            @RequestParam String gatewaySessionId) {
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

    @PutMapping("/{id}/start-service")
    public ResponseEntity<BookingResponseDTO> startService(@PathVariable UUID id) {
        return ResponseEntity.ok(bookingService.startService(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<BookingResponseDTO> updateBookingStatus(
            @PathVariable UUID id,
            @RequestParam com.fixzone.fixzon_backend.enums.BookingStatus status) {
        return ResponseEntity.ok(bookingService.updateBookingStatus(id, status));
    }

    @PutMapping("/{id}/edit")
    public ResponseEntity<BookingResponseDTO> editExistingBooking(
            @PathVariable UUID id,
            @RequestBody BookingRequestDTO request) {
        return ResponseEntity.ok(bookingService.editExistingBooking(id, request));
    }

    @PutMapping("/{id}/assign-lane")
    public ResponseEntity<BookingResponseDTO> assignLane(
            @PathVariable UUID id,
            @RequestParam Integer laneNumber) {
        return ResponseEntity.ok(bookingService.assignLane(id, laneNumber));
    }

    @GetMapping("/{id}/status-history")
    public ResponseEntity<List<com.fixzone.fixzon_backend.DTO.booking.BookingStatusHistoryDTO>> getBookingStatusHistory(
            @PathVariable UUID id) {
        return ResponseEntity.ok(bookingService.getBookingStatusHistory(id));
    }
    // Legacy endpoints removed
}
