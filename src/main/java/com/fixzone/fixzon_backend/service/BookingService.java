package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.booking.BookingRequestDTO;
import com.fixzone.fixzon_backend.DTO.booking.BookingResponseDTO;
import com.fixzone.fixzon_backend.config.AppConstants;
import com.fixzone.fixzon_backend.enums.BookingStatus;
import com.fixzone.fixzon_backend.model.Booking;
import com.fixzone.fixzon_backend.repository.BookingRepository;
import com.fixzone.fixzon_backend.repository.CustomerRepository;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.ServicePackageRepository;
import com.fixzone.fixzon_backend.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final ServiceCenterRepository serviceCenterRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final VehicleRepository vehicleRepository;
    private final PaymentService paymentService;
    private final CustomerRepository customerRepository;

    public BookingService(BookingRepository bookingRepository,
            ServiceCenterRepository serviceCenterRepository,
            ServicePackageRepository servicePackageRepository,
            VehicleRepository vehicleRepository,
            PaymentService paymentService,
            CustomerRepository customerRepository) {
        this.bookingRepository = bookingRepository;
        this.serviceCenterRepository = serviceCenterRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.vehicleRepository = vehicleRepository;
        this.paymentService = paymentService;
        this.customerRepository = customerRepository;
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

    /**
     * Returns all bookings for the currently authenticated customer.
     * Uses the JWT principal (email) to resolve the customer ID.
     */
    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getBookingsForCurrentCustomer(String customerEmail) {
        if (customerEmail == null || customerEmail.isBlank()) {
            throw new IllegalArgumentException("Customer email must not be null");
        }
        com.fixzone.fixzon_backend.model.Customer customer = customerRepository
                .findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerEmail));
        return getBookingsByCustomer(customer.getUserId());
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getBookingsByCustomer(UUID customerId) {
        return bookingRepository.findByCustomerId(Objects.requireNonNull(customerId, "Customer ID must not be null"))
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingResponseDTO createBooking(BookingRequestDTO request, org.springframework.security.core.Authentication authentication) {
        Booking booking = new Booking();
        BeanUtils.copyProperties(Objects.requireNonNull(request, "Request must not be null"), booking);

        boolean isManager = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SERVICE_MANAGER") || a.getAuthority().equals("ROLE_COMPANY_OWNER") || a.getAuthority().equals("ROLE_SYSTEM_ADMIN"));

        if (!isManager) {
            // Resolve customer from JWT email — never trust customerId from the request body
            String customerEmail = authentication.getName();
            com.fixzone.fixzon_backend.model.Customer customer = customerRepository
                    .findByEmail(customerEmail)
                    .orElseThrow(() -> new RuntimeException("Customer not found: " + customerEmail));
            
            // Always set customer from the authenticated user
            booking.setCustomerId(customer.getUserId());
        } else {
            if (request.getCustomerId() == null) {
                throw new RuntimeException("Customer ID is required when a manager creates a booking");
            }
            booking.setCustomerId(request.getCustomerId());
        }

        // Securely fetch centerId and tenantId from the ServicePackage
        if (booking.getPackageId() != null) {
            servicePackageRepository.findById(booking.getPackageId()).ifPresent(pkg -> {
                if (pkg.getServiceCenter() != null) {
                    booking.setCenterId(pkg.getServiceCenter().getCenterId());
                    if (pkg.getServiceCenter().getOwner() != null) {
                        booking.setTenantId(pkg.getServiceCenter().getOwner().getUserId());
                    }
                }
            });
        }
        
        // Use a default tenant ID if not provided (for multi-tenant support)
        if (booking.getTenantId() == null) {
            booking.setTenantId(UUID.fromString(AppConstants.DEFAULT_TENANT_ID));
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
        if (daysBetween < AppConstants.RESCHEDULE_MIN_DAYS_LEFT) {
            log.warn(">>> RESCHEDULE DENIED: Only {} days left.", daysBetween);
            throw new RuntimeException("Cannot reschedule within 3 days of booking date");
        }

        // Check if the new slot is available
        if (isSlotTaken(booking.getCenterId(), newDate, newTime)) {
            log.warn(">>> RESCHEDULE DENIED: Slot already taken at {}", newTime);
            throw new RuntimeException("The selected slot is no longer available");
        }

        log.info(">>> RESCHEDULE APPROVED: Moving to {} at {}", newDate, newTime);
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
        if (daysBetween < AppConstants.RESCHEDULE_MIN_DAYS_LEFT) {
            BigDecimal fee = booking.getBookingFee() != null ? booking.getBookingFee() : BigDecimal.ZERO;
            BigDecimal penalty = fee.multiply(new BigDecimal(AppConstants.PENALTY_PERCENT_5));
            booking.setCancellationPenalty(penalty);
            penaltyPercent = 5.0;
            log.info(">>> APPLYING 5% PENALTY: {}", penalty);
        } else {
            booking.setCancellationPenalty(BigDecimal.ZERO);
            log.info(">>> NO PENALTY APPLIED (More than 3 days)");
        }

        // Trigger Stripe Refund
        if (booking.getGatewaySessionId() != null && booking.getBookingFeePaid()) {
            log.info(">>> TRIGGERING STRIPE REFUND FOR SESSION: {}", booking.getGatewaySessionId());
            boolean refundSuccess = paymentService.refundPayment(booking.getGatewaySessionId(), penaltyPercent);
            if (!refundSuccess) {
                log.error(">>> STRIPE REFUND FAILED! Check Stripe Dashboard.");
            }
        } else {
            booking.setCancellationPenalty(BigDecimal.ZERO);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setIsCancelled(true);
        booking.setCancelledAt(java.time.LocalDateTime.now());
        
        return mapToResponseDTO(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponseDTO editExistingBooking(UUID id, BookingRequestDTO request) {
        Booking booking = bookingRepository.findById(Objects.requireNonNull(id, "ID must not be null"))
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        // Update fields based on request
        if (request.getBookingDate() != null) {
            booking.setBookingDate(request.getBookingDate());
        }
        if (request.getBookingTime() != null) {
            booking.setBookingTime(request.getBookingTime());
        }
        if (request.getSpecialRequest() != null) {
            booking.setSpecialRequest(request.getSpecialRequest());
        }
        
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
    public List<BookingResponseDTO> getUpcomingBookings(UUID customerId) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        List<Booking> bookings;
        if (customerId != null) {
            bookings = bookingRepository.findByCustomerIdAndStatus(customerId, com.fixzone.fixzon_backend.enums.BookingStatus.CONFIRMED);
        } else {
            bookings = bookingRepository.findByStatus(com.fixzone.fixzon_backend.enums.BookingStatus.CONFIRMED);
        }

        return bookings.stream()
                .filter(b -> {
                    java.time.LocalDateTime dt = java.time.LocalDateTime.of(b.getBookingDate(), b.getBookingTime());
                    return !dt.isBefore(now) && b.getBookingDate().equals(now.toLocalDate());
                })
                .sorted(Comparator.comparing(b -> java.time.LocalDateTime.of(b.getBookingDate(), b.getBookingTime())))
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
        // Standard hours: 08:00 to 18:00 (hourly ranges)
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
        if (booking.getCenterId() != null) {
            var scOpt = serviceCenterRepository.findById(booking.getCenterId());
            if (scOpt.isPresent()) {
                dto.setServiceCenterName(scOpt.get().getName());
                dto.setCenterAddress(scOpt.get().getAddress() != null ? scOpt.get().getAddress() : "");
            } else {
                dto.setServiceCenterName("Unknown Service Center");
                dto.setCenterAddress("");
            }
        } else {
            dto.setServiceCenterName("Unknown Service Center");
            dto.setCenterAddress("");
        }

        if (booking.getPackageId() != null) {
            var pkgOpt = servicePackageRepository.findById(booking.getPackageId());
            if (pkgOpt.isPresent()) {
                dto.setPackageName(pkgOpt.get().getName());
            } else {
                dto.setPackageName("Package");
            }
        } else {
            dto.setPackageName("Package");
        }

        if (booking.getVehicleId() != null) {
            var vOpt = vehicleRepository.findById(booking.getVehicleId());
            if (vOpt.isPresent()) {
                var v = vOpt.get();
                String label = (v.getBrand() != null ? v.getBrand() : "") + (v.getPlateNumber() != null ? " - " + v.getPlateNumber() : "");
                dto.setVehicleLabel(label.isBlank() ? "Registered Vehicle" : label);
            } else {
                dto.setVehicleLabel("Registered Vehicle");
            }
        } else {
            dto.setVehicleLabel("Registered Vehicle");
        }

        dto.setIsOnline(booking.getGatewaySessionId() != null && !booking.getGatewaySessionId().isEmpty());
        return dto;
    }
}
