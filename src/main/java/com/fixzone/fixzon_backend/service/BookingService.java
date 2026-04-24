package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.booking.BookingRequestDTO;
import com.fixzone.fixzon_backend.DTO.booking.BookingResponseDTO;
import com.fixzone.fixzon_backend.enums.BookingStatus;
import com.fixzone.fixzon_backend.model.Booking;
import com.fixzone.fixzon_backend.repository.BookingRepository;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.ServicePackageRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ServiceCenterRepository serviceCenterRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final PaymentService paymentService;

    public BookingService(BookingRepository bookingRepository,
            ServiceCenterRepository serviceCenterRepository,
            ServicePackageRepository servicePackageRepository,
            PaymentService paymentService) {
        this.bookingRepository = bookingRepository;
        this.serviceCenterRepository = serviceCenterRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.paymentService = paymentService;
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BookingResponseDTO getBookingById(UUID id) {
        Booking booking = bookingRepository.findById(Objects.requireNonNull(id, "ID must not be null"))
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        return mapToResponseDTO(Objects.requireNonNull(booking));
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getBookingsByCustomer(UUID customerId) {
        return bookingRepository.findByCustomerId(Objects.requireNonNull(customerId, "Customer ID must not be null"))
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingResponseDTO createBooking(BookingRequestDTO request) {
        Booking booking = new Booking();
        BeanUtils.copyProperties(Objects.requireNonNull(request, "Request must not be null"), booking);
        
        // Ensure IDs are set
        if (booking.getBookingId() == null) {
            booking.setBookingId(UUID.randomUUID());
        }
        
        // Use a default tenant ID if not provided (for multi-tenant support)
        if (booking.getTenantId() == null) {
            booking.setTenantId(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        }
        
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        Booking saved = bookingRepository.save(booking);
        return mapToResponseDTO(saved);
    }


    @Transactional
    public BookingResponseDTO rescheduleBooking(UUID id, LocalDate newDate, LocalTime newTime) {
        Booking booking = bookingRepository.findById(Objects.requireNonNull(id, "ID must not be null"))
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new RuntimeException("Cannot reschedule a cancelled or completed booking");
        }

        // Rule: Must be at least 3 days before the original booking date
        long daysBetween = ChronoUnit.DAYS.between(LocalDate.now(), booking.getBookingDate());
        if (daysBetween < 3) {
            System.err.println(">>> RESCHEDULE DENIED: Only " + daysBetween + " days left.");
            throw new RuntimeException("Cannot reschedule within 3 days of booking date");
        }

        // Check if the new slot is available
        if (isSlotTaken(booking.getCenterId(), newDate, newTime)) {
            System.err.println(">>> RESCHEDULE DENIED: Slot already taken at " + newTime);
            throw new RuntimeException("The selected slot is no longer available");
        }

        System.out.println(">>> RESCHEDULE APPROVED: Moving to " + newDate + " at " + newTime);
        booking.setBookingDate(newDate);
        booking.setBookingTime(newTime);
        booking.setRescheduleCount((booking.getRescheduleCount() == null ? 0 : booking.getRescheduleCount()) + 1);
        
        return mapToResponseDTO(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponseDTO cancelBooking(UUID id) {
        Booking booking = bookingRepository.findById(Objects.requireNonNull(id, "ID must not be null"))
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking is already cancelled");
        }

        // Check how many days left
        long daysBetween = ChronoUnit.DAYS.between(LocalDate.now(), booking.getBookingDate());
        
        // Apply 5% penalty if within 3 days
        double penaltyPercent = 0.0;
        if (daysBetween < 3) {
            BigDecimal fee = booking.getBookingFee() != null ? booking.getBookingFee() : BigDecimal.ZERO;
            BigDecimal penalty = fee.multiply(new BigDecimal("0.05"));
            booking.setCancellationPenalty(penalty);
            penaltyPercent = 5.0;
            System.out.println(">>> APPLYING 5% PENALTY: " + penalty);
        } else {
            booking.setCancellationPenalty(BigDecimal.ZERO);
            System.out.println(">>> NO PENALTY APPLIED (More than 3 days)");
        }

        // Trigger Stripe Refund
        if (booking.getGatewaySessionId() != null && booking.getBookingFeePaid()) {
            System.out.println(">>> TRIGGERING STRIPE REFUND FOR SESSION: " + booking.getGatewaySessionId());
            boolean refundSuccess = paymentService.refundPayment(booking.getGatewaySessionId(), penaltyPercent);
            if (!refundSuccess) {
                System.err.println(">>> STRIPE REFUND FAILED! Check Stripe Dashboard.");
            }
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setIsCancelled(true);
        booking.setCancelledAt(LocalDateTime.now());
        
        return mapToResponseDTO(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getBookingsByCenter(UUID centerId) {
        return bookingRepository.findByCenterId(Objects.requireNonNull(centerId, "Center ID must not be null")).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getBookingsByStatus(String status) {
        try {
            BookingStatus bookingStatus = BookingStatus.valueOf(status.toUpperCase());
            return bookingRepository.findByStatus(bookingStatus).stream()
                    .map(this::mapToResponseDTO)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getBookingsByMechanic(UUID mechanicId) {
        return bookingRepository
                .findByAssignedMechanicId(Objects.requireNonNull(mechanicId, "Mechanic ID must not be null")).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isSlotTaken(UUID centerId, LocalDate date, LocalTime time) {
        return bookingRepository.existsActiveSlot(Objects.requireNonNull(centerId, "Center ID must not be null"), date,
                time, java.time.LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<String> getAvailableSlots(UUID centerId, LocalDate date) {
        // Standard hours: 09:00 to 17:00 (hourly)
        List<String> allSlots = List.of("09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00");
        
        return allSlots.stream()
                .filter(slotStr -> !isSlotTaken(centerId, date, LocalTime.parse(slotStr)))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteBooking(UUID id) {
        bookingRepository.deleteById(Objects.requireNonNull(id, "ID must not be null"));
    }

    @Transactional
    public BookingResponseDTO completeBooking(UUID id) {
        Booking booking = bookingRepository.findById(Objects.requireNonNull(id, "ID must not be null"))
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        booking.setStatus(BookingStatus.COMPLETED);
        return mapToResponseDTO(Objects.requireNonNull(bookingRepository.save(booking)));
    }

    @Transactional
    public BookingResponseDTO startService(UUID id) {
        Booking booking = bookingRepository.findById(Objects.requireNonNull(id, "ID must not be null"))
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        booking.setStatus(BookingStatus.IN_PROGRESS);
        return mapToResponseDTO(Objects.requireNonNull(bookingRepository.save(booking)));
    }

    @Transactional
    public BookingResponseDTO completePayment(UUID id, String gatewaySessionId) {
        Booking booking = bookingRepository.findById(Objects.requireNonNull(id, "ID must not be null"))
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        // Update booking with payment info
        booking.setGatewaySessionId(gatewaySessionId);
        booking.setBookingFeePaid(true);
        booking.setStatus(BookingStatus.CONFIRMED); // Transitions to Upcoming
        
        return mapToResponseDTO(Objects.requireNonNull(bookingRepository.save(booking)));
    }

    private BookingResponseDTO mapToResponseDTO(@org.springframework.lang.NonNull Booking booking) {
        Objects.requireNonNull(booking, "Booking must not be null");
        BookingResponseDTO dto = new BookingResponseDTO();
        BeanUtils.copyProperties(booking, dto);
        serviceCenterRepository.findById(Objects.requireNonNull(booking.getCenterId(), "Center ID must not be null"))
                .ifPresent(c -> dto.setServiceCenterName(c.getName()));
        servicePackageRepository.findById(Objects.requireNonNull(booking.getPackageId(), "Package ID must not be null"))
                .ifPresent(p -> dto.setPackageName(p.getName()));
        return dto;
    }
}
