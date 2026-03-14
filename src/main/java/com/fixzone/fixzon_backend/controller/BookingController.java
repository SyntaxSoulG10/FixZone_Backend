package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.BookingDTO;
import com.fixzone.fixzon_backend.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public ResponseEntity<List<BookingDTO>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingDTO> getBookingById(@PathVariable UUID id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    @GetMapping("/center/{centerId}")
    public ResponseEntity<List<BookingDTO>> getBookingsByCenter(@PathVariable UUID centerId) {
        return ResponseEntity.ok(bookingService.getBookingsByCenter(centerId));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<BookingDTO>> getBookingsByCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(bookingService.getBookingsByCustomer(customerId));
    }

    @GetMapping("/mechanic/{mechanicId}")
    public ResponseEntity<List<BookingDTO>> getBookingsByMechanic(@PathVariable UUID mechanicId) {
        return ResponseEntity.ok(bookingService.getBookingsByMechanic(mechanicId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<BookingDTO>> getBookingsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(bookingService.getBookingsByStatus(status));
    }

    @PostMapping
    public ResponseEntity<BookingDTO> createBooking(@RequestBody BookingDTO dto) {
        return ResponseEntity.ok(bookingService.createBooking(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookingDTO> updateBooking(@PathVariable UUID id,
            @RequestBody BookingDTO dto) {
        return ResponseEntity.ok(bookingService.updateBooking(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBooking(@PathVariable UUID id) {
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }
}
