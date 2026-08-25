package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.booking.BookingRequestDTO;
import com.fixzone.fixzon_backend.DTO.booking.BookingResponseDTO;
import com.fixzone.fixzon_backend.config.AppConstants;
import com.fixzone.fixzon_backend.enums.BookingStatus;
import com.fixzone.fixzon_backend.model.Booking;
import com.fixzone.fixzon_backend.repository.BookingRepository;
import com.fixzone.fixzon_backend.repository.CustomerRepository;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
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
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ArrayList;
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
    private final com.fixzone.fixzon_backend.repository.UserRepository userRepository;
    private final OwnerRepository ownerRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final com.fixzone.fixzon_backend.repository.BookingStatusHistoryRepository bookingStatusHistoryRepository;
    private final SchedulingService schedulingService;

    public BookingService(BookingRepository bookingRepository,
            ServiceCenterRepository serviceCenterRepository,
            ServicePackageRepository servicePackageRepository,
            VehicleRepository vehicleRepository,
            PaymentService paymentService,
            CustomerRepository customerRepository,
            com.fixzone.fixzon_backend.repository.UserRepository userRepository,
            OwnerRepository ownerRepository,
            NotificationService notificationService,
            EmailService emailService,
            com.fixzone.fixzon_backend.repository.BookingStatusHistoryRepository bookingStatusHistoryRepository,
            SchedulingService schedulingService) {
        this.bookingRepository = bookingRepository;
        this.serviceCenterRepository = serviceCenterRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.vehicleRepository = vehicleRepository;
        this.paymentService = paymentService;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.ownerRepository = ownerRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.bookingStatusHistoryRepository = bookingStatusHistoryRepository;
        this.schedulingService = schedulingService;
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
    public BookingResponseDTO createBooking(BookingRequestDTO request,
            org.springframework.security.core.Authentication authentication) {
        Booking booking = new Booking();
        BeanUtils.copyProperties(Objects.requireNonNull(request, "Request must not be null"), booking);

        boolean isManager = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SERVICE_MANAGER")
                        || a.getAuthority().equals("ROLE_COMPANY_OWNER")
                        || a.getAuthority().equals("ROLE_SYSTEM_ADMIN"));

        if (!isManager) {
            // Resolve customer from JWT email — never trust customerId from the request
            // body
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

        // Securely fetch centerId and tenantId from the ServicePackage → ServiceCenter
        // → Owner chain.
        // We always look up the Owner entity to ensure stripeAccountId /
        // stripeOnboardingComplete
        // are read from the correct table, regardless of when the branch was created.
        if (booking.getPackageId() != null) {
            servicePackageRepository.findById(booking.getPackageId()).ifPresent(pkg -> {
                if (pkg.getServiceCenter() != null) {
                    booking.setCenterId(pkg.getServiceCenter().getCenterId());
                    if (pkg.getServiceCenter().getOwner() != null) {
                        UUID ownerId = pkg.getServiceCenter().getOwner().getUserId();
                        ownerRepository.findById(ownerId).ifPresent(owner -> {
                            com.fixzone.fixzon_backend.enums.SubscriptionStatus status = com.fixzone.fixzon_backend.enums.SubscriptionStatus
                                    .fromLegacy(owner.getSubscriptionStatus());
                            if (!status.isAccessAllowed()) {
                                throw new RuntimeException(
                                        "Cannot create booking. The service center's subscription has expired.");
                            }
                            booking.setTenantId(owner.getUserId());
                        });
                    }
                }

                // Set the estimated cost based on the package's base price
                if (pkg.getBasePrice() != null) {
                    booking.setEstimatedCost(pkg.getBasePrice());
                }
            });
        }
        if (booking.getBookingId() == null) {
            booking.setBookingId(UUID.randomUUID());
        }

        // Set duration and end time
        int durationMins = booking.getDurationMins() != null ? booking.getDurationMins()
                : schedulingService.resolvePackageDuration(booking.getPackageId());
        booking.setDurationMins(durationMins);
        if (booking.getBookingTime() != null) {
            booking.setEndTime(booking.getBookingTime().plusMinutes(durationMins));
        }

        // Validate booking schedule against working hours, lunch break, and multi-lane capacity
        if (booking.getCenterId() != null && booking.getBookingDate() != null && booking.getBookingTime() != null) {
            boolean available = schedulingService.isSlotAvailable(
                    booking.getCenterId(),
                    booking.getBookingDate(),
                    booking.getBookingTime(),
                    durationMins,
                    null
            );
            if (!available) {
                throw new RuntimeException("Selected time slot is not available or outside working hours");
            }
        }

        // NOTE: We intentionally do NOT fall back to a DEFAULT_TENANT_ID here.
        // If tenantId is null it means the branch has no owner — the payment service
        // will throw a clear error when the customer tries to pay.

        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        Booking saved = bookingRepository.save(booking);

        saveStatusHistory(saved, BookingStatus.PENDING_PAYMENT, "CUSTOMER");

        // Send notifications
        if (saved.getCustomerId() != null) {
            userRepository.findById(saved.getCustomerId()).ifPresent(user -> {
                notificationService.createNotificationSafe(user, "Booking Created",
                        "Your booking has been created. Please complete payment to confirm your slot.", "INFO",
                        "/bookings");
            });
        }
        if (saved.getCenterId() != null) {
            serviceCenterRepository.findById(saved.getCenterId()).ifPresent(sc -> {
                if (sc.getOwner() != null) {
                    notificationService.createNotificationSafe(sc.getOwner(), "New Booking Received",
                            "A new booking was placed at " + sc.getName() + ".", "INFO",
                            "/dashboard/company-owner/centers");
                }
            });
        }

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

        // Check if the new slot is available via SchedulingService
        int durationMins = booking.getDurationMins() != null ? booking.getDurationMins()
                : schedulingService.resolvePackageDuration(booking.getPackageId());

        boolean available = schedulingService.isSlotAvailable(
                booking.getCenterId(),
                newDate,
                newTime,
                durationMins,
                booking.getBookingId()
        );

        if (!available) {
            log.warn(">>> RESCHEDULE DENIED: Slot not available at {}", newTime);
            throw new RuntimeException("The selected slot is no longer available");
        }

        log.info(">>> RESCHEDULE APPROVED: Moving to {} at {}", newDate, newTime);
        booking.setBookingDate(newDate);
        booking.setBookingTime(newTime);
        booking.setDurationMins(durationMins);
        booking.setEndTime(newTime.plusMinutes(durationMins));
        booking.setRescheduleCount((booking.getRescheduleCount() == null ? 0 : booking.getRescheduleCount()) + 1);
        Booking saved = bookingRepository.save(booking);

        // Record history log
        saveStatusHistory(saved, saved.getStatus(), "CUSTOMER_RESCHEDULE");

        // Send reschedule email to customer
        try {
            customerRepository.findById(saved.getCustomerId()).ifPresent(customer -> {
                String centerName = serviceCenterRepository.findById(saved.getCenterId())
                        .map(sc -> sc.getName()).orElse("Service Center");
                emailService.sendBookingRescheduleEmail(
                        customer.getEmail(),
                        customer.getFullName(),
                        saved.getBookingId().toString().substring(0, 8).toUpperCase(),
                        centerName,
                        newDate.toString(),
                        newTime.toString()
                );
            });
        } catch (Exception e) {
            log.warn("Failed to send reschedule email for booking {}: {}", id, e.getMessage());
        }

        // Send in-app notifications asynchronously
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                if (saved.getCustomerId() != null) {
                    customerRepository.findById(saved.getCustomerId()).ifPresent(customer -> {
                        notificationService.createNotificationSafe(customer, "Booking Rescheduled",
                                "Your booking has been rescheduled to " + newDate + " at " + newTime + ".", "INFO",
                                "/bookings");
                    });
                }
                if (saved.getCenterId() != null) {
                    serviceCenterRepository.findById(saved.getCenterId()).ifPresent(sc -> {
                        if (sc.getOwner() != null) {
                            notificationService.createNotificationSafe(sc.getOwner(), "Booking Rescheduled",
                                    "A booking at " + sc.getName() + " was rescheduled to " + newDate + " at " + newTime + ".",
                                    "INFO", "/dashboard/company-owner/centers");
                        }
                    });
                }
            } catch (Exception e) {
                log.error("Failed to send reschedule notifications asynchronously", e);
            }
        });

        return mapToResponseDTO(saved);
    }

    @Transactional
    public BookingResponseDTO cancelBooking(UUID id) {
        Booking booking = bookingRepository.findById(Objects.requireNonNull(id, "ID must not be null"))
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            log.info(">>> BOOKING IS ALREADY CANCELLED: {}", id);
            return mapToResponseDTO(booking);
        }

        // Check how many days left
        long daysBetween = ChronoUnit.DAYS.between(LocalDate.now(), booking.getBookingDate());

        // Apply 5% penalty if within 3 days
        double penaltyPercent = 0.0;
        if (daysBetween < AppConstants.RESCHEDULE_MIN_DAYS_LEFT) {
            BigDecimal fee = booking.getBookingFee() != null ? booking.getBookingFee() : 
                (booking.getEstimatedCost() != null ? booking.getEstimatedCost().multiply(new BigDecimal("0.40")) : BigDecimal.ZERO);
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
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setIsCancelled(true);
        booking.setCancelledAt(java.time.LocalDateTime.now());
        Booking saved = bookingRepository.save(booking);

        // Record history log
        saveStatusHistory(saved, BookingStatus.CANCELLED, "CUSTOMER_CANCEL");

        // Send notifications and cancellation email asynchronously
        BigDecimal fee = saved.getBookingFee() != null ? saved.getBookingFee() :
            (saved.getEstimatedCost() != null ? saved.getEstimatedCost().multiply(new BigDecimal("0.40")) : BigDecimal.ZERO);
        BigDecimal penalty = saved.getCancellationPenalty() != null ? saved.getCancellationPenalty() : BigDecimal.ZERO;
        BigDecimal refundCalc = fee.subtract(penalty);
        if (refundCalc.compareTo(BigDecimal.ZERO) < 0) refundCalc = BigDecimal.ZERO;
        final BigDecimal finalRefund = refundCalc;
        final BigDecimal finalPenalty = penalty;

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                if (saved.getCustomerId() != null) {
                    customerRepository.findById(saved.getCustomerId()).ifPresent(customer -> {
                        notificationService.createNotificationSafe(customer, "Booking Cancelled",
                                "Your booking for " + saved.getBookingDate() + " has been cancelled. Refund: LKR " + finalRefund,
                                "WARNING", "/bookings");

                        // Send cancellation email
                        emailService.sendBookingCancellationEmail(customer.getEmail(), customer.getFullName(),
                                saved.getBookingDate().toString(), finalRefund, finalPenalty);
                    });
                }
                if (saved.getCenterId() != null) {
                    serviceCenterRepository.findById(saved.getCenterId()).ifPresent(sc -> {
                        if (sc.getOwner() != null) {
                            notificationService.createNotificationSafe(sc.getOwner(), "Booking Cancelled",
                                    "A booking at " + sc.getName() + " for " + saved.getBookingDate() + " was cancelled.",
                                    "WARNING", "/dashboard/company-owner/centers");
                        }
                    });
                }
            } catch (Exception e) {
                log.error("Failed to send cancellation notifications/email asynchronously", e);
            }
        });

        return mapToResponseDTO(saved);
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
            bookings = bookingRepository.findByCustomerId(customerId).stream()
                    .filter(b -> b.getStatus() == com.fixzone.fixzon_backend.enums.BookingStatus.CONFIRMED ||
                            b.getStatus() == com.fixzone.fixzon_backend.enums.BookingStatus.PENDING_PAYMENT)
                    .collect(Collectors.toList());
        } else {
            bookings = bookingRepository.findAll().stream()
                    .filter(b -> b.getStatus() == com.fixzone.fixzon_backend.enums.BookingStatus.CONFIRMED ||
                            b.getStatus() == com.fixzone.fixzon_backend.enums.BookingStatus.PENDING_PAYMENT)
                    .collect(Collectors.toList());
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
    public boolean isSlotTakenExcludingBooking(UUID centerId, LocalDate date, LocalTime time, UUID excludeBookingId) {
        if (excludeBookingId == null) {
            return isSlotTaken(centerId, date, time);
        }
        return bookingRepository.existsActiveSlotExcludingBooking(Objects.requireNonNull(centerId, "Center ID must not be null"), date,
                time, excludeBookingId, java.time.LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<String> getAvailableSlots(UUID centerId, LocalDate date) {
        return getAvailableStartTimes(centerId, date, null);
    }

    @Transactional(readOnly = true)
    public List<String> getAvailableStartTimes(UUID centerId, LocalDate date, UUID packageId) {
        return schedulingService.getAvailableStartTimes(centerId, date, packageId);
    }

    @Transactional
    public BookingResponseDTO assignLane(UUID bookingId, Integer laneNumber) {
        Booking booking = bookingRepository.findById(Objects.requireNonNull(bookingId, "ID must not be null"))
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        booking.setAssignedLane(laneNumber);
        return mapToResponseDTO(bookingRepository.save(booking));
    }

    @Transactional
    public void deleteBooking(UUID id) {
        bookingRepository.deleteById(Objects.requireNonNull(id, "ID must not be null"));
    }

    private void saveStatusHistory(Booking booking, BookingStatus status, String changedBy) {
        try {
            com.fixzone.fixzon_backend.model.BookingStatusHistory history = new com.fixzone.fixzon_backend.model.BookingStatusHistory();
            history.setBookingId(booking.getBookingId());
            history.setStatus(status);
            history.setChangedAt(java.time.LocalDateTime.now());
            history.setChangedBy(changedBy != null ? changedBy : "SYSTEM");
            bookingStatusHistoryRepository.save(history);
        } catch (Exception e) {
            log.error("Failed to save booking status history", e);
        }
    }

    @Transactional(readOnly = true)
    public List<com.fixzone.fixzon_backend.DTO.booking.BookingStatusHistoryDTO> getBookingStatusHistory(
            UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        List<com.fixzone.fixzon_backend.model.BookingStatusHistory> historyList = bookingStatusHistoryRepository
                .findByBookingIdOrderByChangedAtAsc(bookingId);

        List<com.fixzone.fixzon_backend.DTO.booking.BookingStatusHistoryDTO> dtos = new java.util.ArrayList<>();

        // 1. If database status history entries exist, return them with their EXACT
        // real changedAt timestamps
        if (historyList != null && !historyList.isEmpty()) {
            return historyList.stream().map(h -> {
                com.fixzone.fixzon_backend.DTO.booking.BookingStatusHistoryDTO dto = new com.fixzone.fixzon_backend.DTO.booking.BookingStatusHistoryDTO();
                dto.setId(h.getId());
                dto.setBookingId(h.getBookingId());
                dto.setStatus(h.getStatus());

                String display = h.getStatus().name().replace('_', ' ');
                if ("CUSTOMER_RESCHEDULE".equals(h.getChangedBy())) {
                    display = "Booking Rescheduled";
                } else if (h.getStatus() == BookingStatus.CONFIRMED) {
                    display = "Ready for Service";
                } else if (h.getStatus() == BookingStatus.IN_PROGRESS) {
                    display = "Service In Progress";
                } else if (h.getStatus() == BookingStatus.COMPLETED) {
                    display = "Service Completed";
                } else if (h.getStatus() == BookingStatus.PENDING_PAYMENT) {
                    display = "Booking Created";
                } else if (h.getStatus() == BookingStatus.CANCELLED) {
                    display = "Booking Cancelled";
                }
                dto.setStatusDisplay(display);
                dto.setChangedAt(h.getChangedAt()); // Exact real timestamp from DB
                dto.setChangedBy(h.getChangedBy());
                return dto;
            }).collect(Collectors.toList());
        }

        // 2. Fallback for legacy bookings where historyList is empty:
        // Use real booking entity timestamps (createdAt, updatedAt, cancelledAt)
        if (booking != null) {
            java.time.LocalDateTime createdAt = booking.getCreatedAt() != null
                    ? booking.getCreatedAt()
                    : java.time.LocalDateTime.now();

            java.time.LocalDateTime updatedAt = booking.getUpdatedAt() != null
                    ? booking.getUpdatedAt()
                    : createdAt;

            BookingStatus current = booking.getStatus() != null ? booking.getStatus() : BookingStatus.PENDING_PAYMENT;

            // Creation Event
            dtos.add(new com.fixzone.fixzon_backend.DTO.booking.BookingStatusHistoryDTO(
                    UUID.randomUUID(), bookingId, BookingStatus.PENDING_PAYMENT, "Booking Created", createdAt,
                    "CUSTOMER"));

            if (booking.getRescheduleCount() != null && booking.getRescheduleCount() > 0) {
                dtos.add(new com.fixzone.fixzon_backend.DTO.booking.BookingStatusHistoryDTO(
                        UUID.randomUUID(), bookingId, current, "Booking Rescheduled", updatedAt,
                        "CUSTOMER_RESCHEDULE"));
            }

            if (current == BookingStatus.CONFIRMED) {
                dtos.add(new com.fixzone.fixzon_backend.DTO.booking.BookingStatusHistoryDTO(
                        UUID.randomUUID(), bookingId, BookingStatus.CONFIRMED, "Ready for Service", updatedAt,
                        "CUSTOMER_PAYMENT"));
            } else if (current == BookingStatus.IN_PROGRESS) {
                dtos.add(new com.fixzone.fixzon_backend.DTO.booking.BookingStatusHistoryDTO(
                        UUID.randomUUID(), bookingId, BookingStatus.IN_PROGRESS, "Service In Progress", updatedAt,
                        "MANAGER"));
            } else if (current == BookingStatus.COMPLETED) {
                dtos.add(new com.fixzone.fixzon_backend.DTO.booking.BookingStatusHistoryDTO(
                        UUID.randomUUID(), bookingId, BookingStatus.COMPLETED, "Service Completed", updatedAt,
                        "MANAGER"));
            } else if (current == BookingStatus.CANCELLED) {
                java.time.LocalDateTime cancelledTime = booking.getCancelledAt() != null ? booking.getCancelledAt()
                        : updatedAt;
                dtos.add(new com.fixzone.fixzon_backend.DTO.booking.BookingStatusHistoryDTO(
                        UUID.randomUUID(), bookingId, BookingStatus.CANCELLED, "Booking Cancelled", cancelledTime,
                        "USER"));
            }
        }

        return dtos;
    }

    @Transactional
    public BookingResponseDTO updateBookingStatus(UUID id, BookingStatus newStatus) {
        Booking booking = bookingRepository.findById(Objects.requireNonNull(id, "ID must not be null"))
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // DUPLICATE PROTECTION: Skip if status hasn't changed
        if (booking.getStatus() == newStatus) {
            log.info(">>> STATUS UPDATE SKIPPED: Booking {} is already in status {}", id, newStatus);
            return mapToResponseDTO(booking);
        }

        booking.setStatus(newStatus);
        Booking saved = bookingRepository.save(booking);

        // Record history log
        saveStatusHistory(saved, newStatus, "MANAGER");

        if (saved.getCustomerId() != null) {
            com.fixzone.fixzon_backend.model.User recipient = userRepository.findById(saved.getCustomerId())
                    .orElseGet(() -> customerRepository.findById(saved.getCustomerId())
                            .map(c -> (com.fixzone.fixzon_backend.model.User) c).orElse(null));

            if (recipient != null) {
                log.info(">>> CREATING STATUS NOTIFICATION FOR USER {}: status = {}", recipient.getUserId(), newStatus);
                if (newStatus == BookingStatus.IN_PROGRESS) {
                    notificationService.createNotificationSafe(recipient, "Service Started",
                            "Work has started on your vehicle service.", "INFO", "/bookings");
                } else if (newStatus == BookingStatus.COMPLETED) {
                    notificationService.createNotificationSafe(recipient, "Service Completed",
                            "Your vehicle service is completed! Please collect your vehicle.", "SUCCESS", "/bookings");
                } else if (newStatus == BookingStatus.CONFIRMED) {
                    notificationService.createNotificationSafe(recipient, "Booking Confirmed",
                            "Your booking has been confirmed and is ready for service.", "INFO", "/bookings");
                } else if (newStatus == BookingStatus.CANCELLED) {
                    notificationService.createNotificationSafe(recipient, "Booking Cancelled",
                            "Your booking for " + saved.getBookingDate() + " was cancelled.", "WARNING", "/bookings");
                } else if (newStatus == BookingStatus.PAID) {
                    notificationService.createNotificationSafe(recipient, "Invoice Paid",
                            "Payment received and invoice generated for your vehicle service.", "SUCCESS", "/bookings");
                }
            } else {
                log.error(">>> FAILED TO CREATE STATUS NOTIFICATION: Customer/User ID {} not found!",
                        saved.getCustomerId());
            }
        }

        return mapToResponseDTO(saved);
    }

    @Transactional
    public BookingResponseDTO completeBooking(UUID id) {
        return updateBookingStatus(id, BookingStatus.COMPLETED);
    }

    @Transactional
    public BookingResponseDTO startService(UUID id) {
        return updateBookingStatus(id, BookingStatus.IN_PROGRESS);
    }

    @Transactional
    public BookingResponseDTO completePayment(UUID id, String gatewaySessionId) {
        Booking booking = bookingRepository.findById(Objects.requireNonNull(id, "ID must not be null"))
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Update booking with payment info
        booking.setGatewaySessionId(gatewaySessionId);
        booking.setBookingFeePaid(true);
        booking.setStatus(BookingStatus.CONFIRMED); // Transitions to Upcoming
        Booking saved = bookingRepository.save(booking);

        // Save status history record
        saveStatusHistory(saved, BookingStatus.CONFIRMED, "CUSTOMER_PAYMENT");

        if (saved.getCustomerId() != null) {
            com.fixzone.fixzon_backend.model.User recipient = userRepository.findById(saved.getCustomerId())
                    .orElseGet(() -> customerRepository.findById(saved.getCustomerId())
                            .map(c -> (com.fixzone.fixzon_backend.model.User) c).orElse(null));

            if (recipient != null) {
                log.info(">>> CREATING PAYMENT SUCCESSFUL NOTIFICATION FOR USER {}", recipient.getUserId());
                notificationService.createNotificationSafe(recipient, "Payment Successful",
                        "Your booking payment has been confirmed.", "SUCCESS", "/bookings");
            } else {
                log.error(">>> FAILED TO CREATE PAYMENT NOTIFICATION: Customer ID {} not found!",
                        saved.getCustomerId());
            }
        }

        return mapToResponseDTO(saved);
    }

    @Transactional
    public List<BookingResponseDTO> processOverdueBookings() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        List<Booking> overdueBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .filter(b -> {
                    if (b.getBookingDate() == null || b.getBookingTime() == null) return false;
                    java.time.LocalDateTime bookingStart = java.time.LocalDateTime.of(b.getBookingDate(), b.getBookingTime());
                    java.time.LocalDateTime gracePeriodEnd = bookingStart.plusMinutes(30);
                    return now.isAfter(gracePeriodEnd);
                })
                .collect(Collectors.toList());

        List<BookingResponseDTO> cancelledDTOs = new ArrayList<>();

        for (Booking booking : overdueBookings) {
            log.info(">>> AUTO-CANCELLING OVERDUE BOOKING {}: Scheduled for {} {}, 30-minute grace period expired.",
                    booking.getBookingId(), booking.getBookingDate(), booking.getBookingTime());

            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancelledAt(now);

            BigDecimal baseCost = booking.getBookingFee() != null ? booking.getBookingFee() : (booking.getEstimatedCost() != null ? booking.getEstimatedCost() : BigDecimal.ZERO);
            BigDecimal penaltyAmount = baseCost.multiply(new BigDecimal("0.90"));
            booking.setCancellationPenalty(penaltyAmount);

            Booking saved = bookingRepository.save(booking);

            String logMsg = String.format("Auto-Cancelled: Customer No-Show after 30-Minute Grace Period (90%% Penalty Applied: LKR %.2f)", penaltyAmount.doubleValue());
            saveStatusHistory(saved, BookingStatus.CANCELLED, logMsg);

            if (saved.getCustomerId() != null) {
                com.fixzone.fixzon_backend.model.User recipient = userRepository.findById(saved.getCustomerId())
                        .orElseGet(() -> customerRepository.findById(saved.getCustomerId())
                                .map(c -> (com.fixzone.fixzon_backend.model.User) c).orElse(null));

                if (recipient != null) {
                    notificationService.createNotificationSafe(
                            recipient,
                            "Booking Auto-Cancelled",
                            "Your booking for " + saved.getBookingDate() + " at " + saved.getBookingTime() +
                                    " was cancelled due to no-show after the 30-minute grace period. A 90% penalty has been applied.",
                            "WARNING",
                            "/bookings"
                    );
                }
            }
            cancelledDTOs.add(mapToResponseDTO(saved));
        }

        return cancelledDTOs;
    }

    private BookingResponseDTO mapToResponseDTO(@org.springframework.lang.NonNull Booking booking) {
        Objects.requireNonNull(booking, "Booking must not be null");
        BookingResponseDTO dto = new BookingResponseDTO();
        BeanUtils.copyProperties(booking, dto);
        // 1. Service Center
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

        // 2. Service Package & Duration / End Time Calculation
        int resolvedDuration = 60;
        if (booking.getDurationMins() != null && booking.getDurationMins() > 0) {
            resolvedDuration = booking.getDurationMins();
        }

        if (booking.getPackageId() != null) {
            var pkgOpt = servicePackageRepository.findById(booking.getPackageId());
            if (pkgOpt.isPresent()) {
                var pkg = pkgOpt.get();
                dto.setPackageName(pkg.getName());
                dto.setPackageDescription(pkg.getDescription());
                if (pkg.getEstimatedDurationMins() != null) {
                    dto.setEstimatedDurationMins(pkg.getEstimatedDurationMins());
                    if (booking.getDurationMins() == null) {
                        resolvedDuration = pkg.getEstimatedDurationMins();
                    }
                }
            } else {
                dto.setPackageName("Package");
            }
        } else {
            dto.setPackageName("Package");
        }

        dto.setDurationMins(resolvedDuration);
        dto.setEstimatedDurationMins(resolvedDuration);
        if (booking.getEndTime() != null) {
            dto.setEndTime(booking.getEndTime());
        } else if (booking.getBookingTime() != null) {
            dto.setEndTime(booking.getBookingTime().plusMinutes(resolvedDuration));
        }

        dto.setAssignedLane(booking.getAssignedLane());

        // 3. Customer Full Name from User/Customer entity
        if (booking.getCustomerId() != null) {
            var userOpt = userRepository.findById(booking.getCustomerId());
            if (userOpt.isPresent()) {
                dto.setCustomerName(userOpt.get().getFullName());
            } else {
                var custOpt = customerRepository.findById(booking.getCustomerId());
                dto.setCustomerName(custOpt.map(com.fixzone.fixzon_backend.model.User::getFullName).orElse("Customer"));
            }
        } else {
            dto.setCustomerName("Customer");
        }

        // 4. Vehicle Brand, Model & Registration / Plate Number
        if (booking.getVehicleId() != null) {
            var vOpt = vehicleRepository.findById(booking.getVehicleId());
            if (vOpt.isPresent()) {
                var v = vOpt.get();
                String vName = ((v.getBrand() != null ? v.getBrand() : "") + (v.getModel() != null ? " " + v.getModel() : "")).trim();
                dto.setVehicleName(vName.isBlank() ? "Vehicle" : vName);
                dto.setPlateNumber(v.getPlateNumber() != null ? v.getPlateNumber() : "");
                
                String label = (v.getBrand() != null ? v.getBrand() : "")
                        + (v.getPlateNumber() != null ? " - " + v.getPlateNumber() : "");
                dto.setVehicleLabel(label.isBlank() ? "Registered Vehicle" : label);
            } else {
                dto.setVehicleName("Vehicle");
                dto.setPlateNumber("");
                dto.setVehicleLabel("Registered Vehicle");
            }
        } else {
            dto.setVehicleName("Vehicle");
            dto.setPlateNumber("");
            dto.setVehicleLabel("Registered Vehicle");
        }

        if (dto.getEstimatedCost() != null && dto.getBookingFee() == null) {
            dto.setBookingFee(dto.getEstimatedCost().multiply(new java.math.BigDecimal("0.40")));
        }

        dto.setIsOnline(booking.getGatewaySessionId() != null && !booking.getGatewaySessionId().isEmpty());
        return dto;
    }
}
