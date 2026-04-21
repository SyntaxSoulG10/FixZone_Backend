package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.booking.BookingRequestDTO;
import com.fixzone.fixzon_backend.DTO.booking.BookingResponseDTO;
import com.fixzone.fixzon_backend.enums.BookingAction;
import com.fixzone.fixzon_backend.enums.BookingStatus;
import com.fixzone.fixzon_backend.model.Booking;
import com.fixzone.fixzon_backend.model.BookingHistory;
import com.fixzone.fixzon_backend.model.ServicePackage;
import com.fixzone.fixzon_backend.repository.BookingHistoryRepository;
import com.fixzone.fixzon_backend.repository.BookingRepository;
import com.fixzone.fixzon_backend.repository.ServicePackageRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final BookingHistoryRepository bookingHistoryRepository;

    public BookingService(
            BookingRepository bookingRepository,
            ServicePackageRepository servicePackageRepository,
            BookingHistoryRepository bookingHistoryRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.bookingHistoryRepository = bookingHistoryRepository;
    }

    public List<BookingResponseDTO> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public BookingResponseDTO getBookingById(UUID id) {
        Objects.requireNonNull(id, "ID must not be null");
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + id));
        return mapToResponseDTO(booking);
    }

    public List<BookingResponseDTO> getBookingsByCenter(UUID centerId) {
        return bookingRepository.findByCenterId(centerId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<BookingResponseDTO> getBookingsByCustomer(UUID customerId) {
        return bookingRepository.findByCustomerId(customerId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }
    // Removed getBookingsByMechanic as it's not currently in our production repository
    public List<BookingResponseDTO> getBookingsByMechanic(UUID mechanicId) {

        if (mechanicId == null) {
            throw new IllegalArgumentException("Mechanic ID must not be null");
        }

        return bookingRepository.findByAssignedMechanicId(mechanicId).stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    public List<BookingResponseDTO> getBookingsByStatus(String status) {
        if (status == null) return List.of();  // avoid null pointer exception
        
        String statusStr = status.trim().toUpperCase();
        if ("PENDING".equals(statusStr)) {
            statusStr = "PENDING_PAYMENT";
        }

        try {
            BookingStatus bookingStatus = BookingStatus.valueOf(statusStr);
            return bookingRepository.findByStatus(bookingStatus).stream()
                    .map(this::mapToResponseDTO)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    public void deleteBooking(UUID id) {
        Objects.requireNonNull(id, "ID must not be null");
        bookingRepository.deleteById(id);
    }

    // Legacy conversion methods removed in favor of mapToResponseDTO and production flow


    /*
      Create a new booking with production-level validations and pricing.
     */
    public BookingResponseDTO createBooking(BookingRequestDTO request) {
        UUID customerId = getCurrentUserId();
        UUID tenantId = getCurrentTenantId();

        // 1. Validate Service Package
        ServicePackage servicePackage = servicePackageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new RuntimeException("Service package not found"));

        // 2. Validate Slot Availability (Soft Lock check)
        boolean slotTaken = bookingRepository.existsByCenterIdAndBookingDateAndBookingTimeAndStatusIn(
                request.getCenterId(),
                request.getBookingDate(),
                request.getBookingTime(),
                List.of(BookingStatus.PENDING_PAYMENT, BookingStatus.CONFIRMED)
        );

        if (slotTaken) {
            throw new RuntimeException("Selected time slot is already booked or pending payment");
        }

        // 3. Date Validations
        if (request.getBookingDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Booking date cannot be in the past");
        }

        // 4. Calculate Pricing (10% booking fee)
        BigDecimal estimatedCost = servicePackage.getBasePrice();
        BigDecimal bookingFee = estimatedCost
                .multiply(BigDecimal.valueOf(0.10))
                .setScale(2, RoundingMode.HALF_UP);

        // 5. Create and Save Booking
        Booking booking = new Booking();
        booking.setBookingId(UUID.randomUUID());
        booking.setTenantId(tenantId);
        booking.setCenterId(request.getCenterId());
        booking.setCustomerId(customerId);
        booking.setVehicleId(request.getVehicleId());
        booking.setPackageId(request.getPackageId());
        booking.setBookingDate(request.getBookingDate());
        booking.setBookingTime(request.getBookingTime());
        booking.setSpecialRequest(request.getSpecialRequest());
        booking.setEstimatedCost(estimatedCost);
        booking.setBookingFee(bookingFee);
        booking.setCancellationPenalty(BigDecimal.ZERO);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setIsPaid(false);
        booking.setIsCancelled(false);
        booking.setRescheduleCount(0);
        booking.setCreatedBy(customerId.toString());
        booking.setUpdatedBy(customerId.toString());

        Booking savedBooking = bookingRepository.save(booking);

        // 6. Audit Logging
        saveBookingHistory(
                savedBooking.getBookingId(),
                tenantId,
                BookingAction.CREATED,
                null,
                null,
                savedBooking.getBookingDate(),
                savedBooking.getBookingTime(),
                BigDecimal.ZERO,
                "Booking created via mobile/web flow"
        );

        return mapToResponseDTO(savedBooking);
    }

    /**
     * Reschedule booking with 3-day restriction rule.
     */
    public BookingResponseDTO rescheduleBooking(UUID bookingId, LocalDate newDate, LocalTime newTime) {
        UUID currentUserId = getCurrentUserId();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // TEMPORARY: Skipping ownership validation for Postman testing (no authentication/JWT yet)
        // validateBookingOwnership(booking, currentUserId);
        //validateBookingOwnership(booking, currentUserId);

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new RuntimeException("Cannot reschedule a cancelled or completed booking");
        }

        // 3-day rule check
        LocalDate today = LocalDate.now();
        LocalDate penaltyStartDate = booking.getBookingDate().minusDays(3);

        // If within last 3 days → block rescheduling
        if (!today.isBefore(penaltyStartDate)) {
            throw new RuntimeException("Rescheduling is not allowed within 3 days of the booking date");
        }

        boolean slotTaken = bookingRepository.existsByCenterIdAndBookingDateAndBookingTimeAndStatusIn(
                booking.getCenterId(),
                newDate,
                newTime,
                List.of(BookingStatus.PENDING_PAYMENT, BookingStatus.CONFIRMED)
        );

        if (slotTaken) {
            throw new RuntimeException("Selected new time slot is already booked");
        }

        LocalDate oldDate = booking.getBookingDate();
        LocalTime oldTime = booking.getBookingTime();

        booking.setBookingDate(newDate);
        booking.setBookingTime(newTime);
        booking.setRescheduleCount(booking.getRescheduleCount() == null ? 1 : booking.getRescheduleCount() + 1);
        booking.setUpdatedBy(currentUserId.toString());

        Booking updatedBooking = bookingRepository.save(booking);

        saveBookingHistory(
                updatedBooking.getBookingId(),
                updatedBooking.getTenantId(),
                BookingAction.RESCHEDULED,
                oldDate,
                oldTime,
                newDate,
                newTime,
                BigDecimal.ZERO,
                "Booking rescheduled"
        );

        return mapToResponseDTO(updatedBooking);
    }

    /**
     * Cancel booking with penalty logic.
     */
    public BookingResponseDTO cancelBooking(UUID bookingId) {
        UUID currentUserId = getCurrentUserId();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // TEMPORARY: Skipping ownership validation for Postman testing (no authentication/JWT yet)
        // validateBookingOwnership(booking, currentUserId);
        //validateBookingOwnership(booking, currentUserId);

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new RuntimeException("Booking is already cancelled or completed");
        }

        BigDecimal penalty = BigDecimal.ZERO;

        // Within 3 days => 5% penalty       *half of booking fee
        LocalDate today = LocalDate.now();
        LocalDate penaltyStartDate = booking.getBookingDate().minusDays(3);

        // Within last 3 days → apply penalty
        if (!today.isBefore(penaltyStartDate)) {
            penalty = booking.getEstimatedCost()
                    .multiply(BigDecimal.valueOf(0.05))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setIsCancelled(true);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancellationPenalty(penalty);
        booking.setUpdatedBy(currentUserId.toString());

        Booking updatedBooking = bookingRepository.save(booking);

        saveBookingHistory(
                updatedBooking.getBookingId(),
                updatedBooking.getTenantId(),
                BookingAction.CANCELLED,
                updatedBooking.getBookingDate(),
                updatedBooking.getBookingTime(),
                null,
                null,
                penalty,
                "Booking cancelled"
        );

        return mapToResponseDTO(updatedBooking);
    }

    /**
     * Mark booking payment as completed and CONFIRM the slot.
     */
    public BookingResponseDTO completePayment(UUID bookingId, UUID paymentId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (Boolean.TRUE.equals(booking.getIsPaid())) {
            throw new RuntimeException("Payment already completed for this booking");
        }

        booking.setIsPaid(true);
        booking.setPaymentId(paymentId);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUpdatedBy(getCurrentUserId().toString());

        Booking updatedBooking = bookingRepository.save(booking);

        saveBookingHistory(
                updatedBooking.getBookingId(),
                updatedBooking.getTenantId(),
                BookingAction.PAYMENT_COMPLETED,
                null,
                null,
                null,
                null,
                BigDecimal.ZERO,
                "Payment successfully completed"
        );

        return mapToResponseDTO(updatedBooking);
    }

    /**
     * Mark job completed (Operationally).
     */
    public BookingResponseDTO completeBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException("Only confirmed bookings can be completed");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setUpdatedBy(getCurrentUserId().toString());

        Booking updatedBooking = bookingRepository.save(booking);

        saveBookingHistory(
                updatedBooking.getBookingId(),
                updatedBooking.getTenantId(),
                BookingAction.COMPLETED,
                null,
                null,
                null,
                null,
                BigDecimal.ZERO,
                "Booking service marked as completed"
        );

        return mapToResponseDTO(updatedBooking);
    }

    // =========================
    // Private Helpers
    // =========================

    private BookingResponseDTO mapToResponseDTO(Booking booking) {
        BookingResponseDTO response = new BookingResponseDTO();
        response.setBookingId(booking.getBookingId());
        response.setCenterId(booking.getCenterId());
        response.setVehicleId(booking.getVehicleId());
        response.setPackageId(booking.getPackageId());
        response.setBookingDate(booking.getBookingDate());
        response.setBookingTime(booking.getBookingTime());
        response.setStatus(booking.getStatus());
        response.setEstimatedCost(booking.getEstimatedCost());
        response.setBookingFee(booking.getBookingFee());
        response.setCancellationPenalty(booking.getCancellationPenalty());
        response.setIsPaid(booking.getIsPaid());
        response.setSpecialRequest(booking.getSpecialRequest());
        response.setCreatedAt(booking.getCreatedAt());
        return response;
    }

    private void saveBookingHistory(
            UUID bookingId,
            UUID tenantId,
            BookingAction action,
            LocalDate oldDate,
            LocalTime oldTime,
            LocalDate newDate,
            LocalTime newTime,
            BigDecimal penalty,
            String note
    ) {
        BookingHistory history = new BookingHistory();
        history.setBookingId(bookingId);
        history.setTenantId(tenantId);
        history.setAction(action);
        history.setOldDate(oldDate);
        history.setOldTime(oldTime);
        history.setNewDate(newDate);
        history.setNewTime(newTime);
        history.setPenalty(penalty);
        history.setNote(note);
        bookingHistoryRepository.save(history);
    }

    private void validateBookingOwnership(Booking booking, UUID currentUserId) {
        if (!booking.getCustomerId().equals(currentUserId)) {
            throw new RuntimeException("Access denied: You do not own this booking");
        }
    }

    private UUID getCurrentUserId() {     // review after JWT set
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            // Temporary fallback for development if auth is not ready
            return UUID.fromString("00000000-0000-0000-0000-000000000001");
        }
        
        Object principal = auth.getPrincipal();
        if (principal instanceof UUID) return (UUID) principal;
        if (principal instanceof String) {
            try {
                return UUID.fromString((String) principal);
            } catch (Exception e) {
                return UUID.randomUUID();
            }
        }
        return UUID.randomUUID();
    }

    private UUID getCurrentTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Placeholder: extract from JWT details/claims later
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }
}
