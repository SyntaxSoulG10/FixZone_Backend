package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.booking.BookingRequestDTO;
import com.fixzone.fixzon_backend.DTO.booking.BookingResponseDTO;
import com.fixzone.fixzon_backend.config.AppConstants;
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

    // Repository for booking database operations
    private final BookingRepository bookingRepository;
    
    // Repository for service center information
    private final ServiceCenterRepository serviceCenterRepository;
    
    // Repository for service package information
    private final ServicePackageRepository servicePackageRepository;
    
    // Service for handling payment operations
    private final PaymentService paymentService;

    // Constructor-based dependency injection
    public BookingService(BookingRepository bookingRepository,
            ServiceCenterRepository serviceCenterRepository,
            ServicePackageRepository servicePackageRepository,
            PaymentService paymentService) {
        this.bookingRepository = bookingRepository;
        this.serviceCenterRepository = serviceCenterRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.paymentService = paymentService;
    }

    // Retrieves all bookings
    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    // Retrieves a booking by ID
    @Transactional(readOnly = true)
    public BookingResponseDTO getBookingById(UUID id) {
        Booking booking = bookingRepository.findById(Objects.requireNonNull(id, "ID must not be null"))
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        return mapToResponseDTO(Objects.requireNonNull(booking));
    }

    // Retrieves all bookings for a specific customer
    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getBookingsByCustomer(UUID customerId) {
        return bookingRepository.findByCustomerId(Objects.requireNonNull(customerId, "Customer ID must not be null"))
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    // Creates a new booking with initial PENDING_PAYMENT status
    @Transactional
    public BookingResponseDTO createBooking(BookingRequestDTO request) {
        Booking booking = new Booking();
        BeanUtils.copyProperties(Objects.requireNonNull(request, "Request must not be null"), booking);
        
        if (booking.getBookingId() == null) {
            booking.setBookingId(UUID.randomUUID());
        }
        
        if (booking.getTenantId() == null) {
            booking.setTenantId(UUID.fromString(AppConstants.DEFAULT_TENANT_ID));
        }
        
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        Booking saved = bookingRepository.save(booking);
        return mapToResponseDTO(saved);
    }


    // Reschedules booking (enforces 3-day minimum before booking date)
    @Transactional
    public BookingResponseDTO rescheduleBooking(UUID id, LocalDate newDate, LocalTime newTime) {
        Booking booking = bookingRepository.findById(Objects.requireNonNull(id, "ID must not be null"))
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new RuntimeException("Cannot reschedule a cancelled or completed booking");
        }

        long daysBetween = ChronoUnit.DAYS.between(LocalDate.now(), booking.getBookingDate());
        if (daysBetween < AppConstants.RESCHEDULE_MIN_DAYS_LEFT) {
            System.err.println(">>> RESCHEDULE DENIED: Only " + daysBetween + " days left.");
            throw new RuntimeException("Cannot reschedule within 3 days of booking date");
        }

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

    // Cancels booking (applies 5% penalty if within 3 days, triggers Stripe refund)
    @Transactional
    public BookingResponseDTO cancelBooking(UUID id) {
        Booking booking = bookingRepository.findById(Objects.requireNonNull(id, "ID must not be null"))
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking is already cancelled");
        }

        long daysBetween = ChronoUnit.DAYS.between(LocalDate.now(), booking.getBookingDate());
        
        double penaltyPercent = 0.0;
        if (daysBetween < AppConstants.RESCHEDULE_MIN_DAYS_LEFT) {
            BigDecimal fee = booking.getBookingFee() != null ? booking.getBookingFee() : BigDecimal.ZERO;
            BigDecimal penalty = fee.multiply(new BigDecimal(AppConstants.PENALTY_PERCENT_5));
            booking.setCancellationPenalty(penalty);
            penaltyPercent = 5.0;
            System.out.println(">>> APPLYING 5% PENALTY: " + penalty);
        } else {
            booking.setCancellationPenalty(BigDecimal.ZERO);
            System.out.println(">>> NO PENALTY APPLIED (More than 3 days)");
        }

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

    // Retrieves all bookings for a service center
    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getBookingsByCenter(UUID centerId) {
        return bookingRepository.findByCenterId(Objects.requireNonNull(centerId, "Center ID must not be null")).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    // Retrieves bookings by status (returns empty list if invalid status)
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

    // Retrieves bookings assigned to a specific mechanic
    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getBookingsByMechanic(UUID mechanicId) {
        return bookingRepository
                .findByAssignedMechanicId(Objects.requireNonNull(mechanicId, "Mechanic ID must not be null")).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    // Checks if time slot is already booked at service center
    @Transactional(readOnly = true)
    public boolean isSlotTaken(UUID centerId, LocalDate date, LocalTime time) {
        return bookingRepository.existsActiveSlot(Objects.requireNonNull(centerId, "Center ID must not be null"), date,
                time, java.time.LocalDateTime.now());
    }

    // Retrieves available hourly slots (08:00-18:00) excluding booked times
    @Transactional(readOnly = true)
    public List<String> getAvailableSlots(UUID centerId, LocalDate date) {
        List<String> allSlots = List.of(
            "08:00-09:00", "09:00-10:00", "10:00-11:00", "11:00-12:00", 
            "12:00-13:00", "13:00-14:00", "14:00-15:00", "15:00-16:00", 
            "16:00-17:00", "17:00-18:00"
        );
        
        return allSlots.stream()
                .filter(slotStr -> {
                    String startTime = slotStr.split("-")[0];
                    return !isSlotTaken(centerId, date, LocalTime.parse(startTime));
                })
                .collect(Collectors.toList());
    }

    // Deletes booking from database
    @Transactional
    public void deleteBooking(UUID id) {
        bookingRepository.deleteById(Objects.requireNonNull(id, "ID must not be null"));
    }

    // Transitions booking to COMPLETED status
    @Transactional
    public BookingResponseDTO completeBooking(UUID id) {
        Booking booking = bookingRepository.findById(Objects.requireNonNull(id, "ID must not be null"))
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        booking.setStatus(BookingStatus.COMPLETED);
        return mapToResponseDTO(Objects.requireNonNull(bookingRepository.save(booking)));
    }

    // Transitions booking to IN_PROGRESS status (service started)
    @Transactional
    public BookingResponseDTO startService(UUID id) {
        Booking booking = bookingRepository.findById(Objects.requireNonNull(id, "ID must not be null"))
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        booking.setStatus(BookingStatus.IN_PROGRESS);
        return mapToResponseDTO(Objects.requireNonNull(bookingRepository.save(booking)));
    }

    // Completes payment and marks booking as CONFIRMED (records Stripe session ID)
    @Transactional
    public BookingResponseDTO completePayment(UUID id, String gatewaySessionId) {
        Booking booking = bookingRepository.findById(Objects.requireNonNull(id, "ID must not be null"))
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        booking.setGatewaySessionId(gatewaySessionId);
        booking.setBookingFeePaid(true);
        booking.setStatus(BookingStatus.CONFIRMED);
        return mapToResponseDTO(Objects.requireNonNull(bookingRepository.save(booking)));
    }

    // Converts entity to DTO with enriched center and package names
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
