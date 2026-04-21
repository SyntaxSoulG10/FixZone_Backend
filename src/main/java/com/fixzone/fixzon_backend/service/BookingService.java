package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.BookingDTO;
import com.fixzone.fixzon_backend.model.Booking;
import com.fixzone.fixzon_backend.repository.BookingRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public List<BookingDTO> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public BookingDTO getBookingById(UUID id) {
        Objects.requireNonNull(id, "ID must not be null");
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + id));
        return convertToDTO(booking);
    }

    public List<BookingDTO> getBookingsByCenter(UUID centerId) {
        return bookingRepository.findByCenterId(centerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<BookingDTO> getBookingsByCustomer(UUID customerId) {
        return bookingRepository.findByCustomerId(customerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<BookingDTO> getBookingsByMechanic(UUID mechanicId) {
        return bookingRepository.findByAssignedMechanicId(mechanicId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<BookingDTO> getBookingsByStatus(String status) {
        return bookingRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public BookingDTO createBooking(BookingDTO dto) {
        Booking booking = convertToEntity(dto);
        if (booking.getBookingId() == null) {
            booking.setBookingId(UUID.randomUUID());
        }
        return convertToDTO(bookingRepository.save(booking));
    }

    public BookingDTO updateBooking(UUID id, BookingDTO dto) {
        Objects.requireNonNull(id, "ID must not be null");
        Booking existing = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + id));

        if (dto != null) {
            existing.setCenterId(dto.getCenterId());
            existing.setTenantId(dto.getTenantId());
            existing.setCustomerId(dto.getCustomerId());
            existing.setVehicleId(dto.getVehicleId());
            existing.setPackageId(dto.getPackageId());
            existing.setPreferredDateTime(dto.getPreferredDateTime());
            existing.setAssignedMechanicId(dto.getAssignedMechanicId());
            existing.setStatus(dto.getStatus());
            existing.setPriority(dto.getPriority());
            existing.setEstimatedCost(dto.getEstimatedCost());
            existing.setUpdatedBy(dto.getUpdatedBy());
        }

        Booking saved = bookingRepository.save(existing);
        return convertToDTO(saved);
    }

    public void deleteBooking(UUID id) {
        Objects.requireNonNull(id, "ID must not be null");
        bookingRepository.deleteById(id);
    }

    private BookingDTO convertToDTO(Booking booking) {
        Objects.requireNonNull(booking, "Booking must not be null");
        return new BookingDTO(
                booking.getBookingId(),
                booking.getCenterId(),
                booking.getTenantId(),
                booking.getCustomerId(),
                booking.getVehicleId(),
                booking.getPackageId(),
                booking.getPreferredDateTime(),
                booking.getAssignedMechanicId(),
                booking.getStatus(),
                booking.getPriority(),
                booking.getEstimatedCost(),
                booking.getCreatedAt(),
                booking.getCreatedBy(),
                booking.getUpdatedAt(),
                booking.getUpdatedBy()
        );
    }

    private Booking convertToEntity(BookingDTO dto) {
        Booking booking = new Booking();
        booking.setBookingId(dto.getBookingId());
        booking.setCenterId(dto.getCenterId());
        booking.setTenantId(dto.getTenantId());
        booking.setCustomerId(dto.getCustomerId());
        booking.setVehicleId(dto.getVehicleId());
        booking.setPackageId(dto.getPackageId());
        booking.setPreferredDateTime(dto.getPreferredDateTime());
        booking.setAssignedMechanicId(dto.getAssignedMechanicId());
        booking.setStatus(dto.getStatus());
        booking.setPriority(dto.getPriority());
        booking.setEstimatedCost(dto.getEstimatedCost());
        booking.setCreatedBy(dto.getCreatedBy());
        booking.setUpdatedBy(dto.getUpdatedBy());
        return booking;
    }
}
