package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.booking.BookingRequestDTO;
import com.fixzone.fixzon_backend.DTO.booking.BookingResponseDTO;
import com.fixzone.fixzon_backend.enums.BookingStatus;
import com.fixzone.fixzon_backend.model.*;
import com.fixzone.fixzon_backend.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService Unit Tests")
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock ServiceCenterRepository serviceCenterRepository;
    @Mock ServicePackageRepository servicePackageRepository;
    @Mock VehicleRepository vehicleRepository;
    @Mock PaymentService paymentService;
    @Mock CustomerRepository customerRepository;
    @Mock OwnerRepository ownerRepository;
    @Mock EmailService emailService;
    @Mock SchedulingService schedulingService;
    @Mock UserRepository userRepository;
    @Mock NotificationService notificationService;
    @Mock BookingStatusHistoryRepository bookingStatusHistoryRepository;

    @InjectMocks BookingService bookingService;

    UUID customerId, centerId, packageId, vehicleId, ownerId;
    Customer customer;
    ServiceCenter serviceCenter;
    ServicePackage servicePackage;
    Owner owner;
    Booking confirmedBooking;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        centerId   = UUID.randomUUID();
        packageId  = UUID.randomUUID();
        vehicleId  = UUID.randomUUID();
        ownerId    = UUID.randomUUID();

        customer = new Customer();
        customer.setUserId(customerId);
        customer.setEmail("customer@test.com");

        owner = new Owner();
        owner.setUserId(ownerId);
        owner.setSubscriptionStatus("TRIAL_ACTIVE");
        owner.setStripeAccountId("acct_test");
        owner.setStripeOnboardingComplete(true);

        User ownerUser = new User();
        ownerUser.setUserId(ownerId);

        serviceCenter = new ServiceCenter();
        serviceCenter.setCenterId(centerId);
        serviceCenter.setName("Test Center");
        serviceCenter.setOwner(ownerUser);

        servicePackage = new ServicePackage();
        servicePackage.setPackageId(packageId);
        servicePackage.setName("Full Service");
        servicePackage.setBasePrice(new BigDecimal("5000.00"));
        servicePackage.setServiceCenter(serviceCenter);

        confirmedBooking = new Booking();
        confirmedBooking.setBookingId(UUID.randomUUID());
        confirmedBooking.setCustomerId(customerId);
        confirmedBooking.setCenterId(centerId);
        confirmedBooking.setPackageId(packageId);
        confirmedBooking.setVehicleId(vehicleId);
        confirmedBooking.setTenantId(ownerId);
        confirmedBooking.setBookingDate(LocalDate.now().plusDays(5));
        confirmedBooking.setBookingTime(LocalTime.of(10, 0));
        confirmedBooking.setStatus(BookingStatus.CONFIRMED);
        confirmedBooking.setBookingFeePaid(true);
        confirmedBooking.setGatewaySessionId("cs_test_session");
        confirmedBooking.setBookingFee(new BigDecimal("2000.00"));
        confirmedBooking.setExpiresAt(LocalDateTime.now().plusHours(1));

        lenient().when(schedulingService.isSlotAvailable(any(), any(), any(), anyInt(), any())).thenReturn(true);
        lenient().when(schedulingService.resolvePackageDuration(any())).thenReturn(60);
    }

    // ── createBooking() ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Customer ID is resolved from JWT, not from request body")
    void customerIdResolvedFromJwt() {
        BookingRequestDTO request = buildRequest();
        request.setCustomerId(UUID.randomUUID()); // should be ignored
        Authentication auth = customerAuth();

        when(customerRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(customer));
        when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingResponseDTO result = bookingService.createBooking(request, auth);

        assertThat(result.getCustomerId()).isEqualTo(customerId);
    }

    @Test
    @DisplayName("Booking saved with PENDING_PAYMENT status")
    void bookingCreatedWithPendingPaymentStatus() {
        Authentication auth = customerAuth();
        when(customerRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(customer));
        when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        bookingService.createBooking(buildRequest(), auth);

        verify(bookingRepository).save(argThat(b -> b.getStatus() == BookingStatus.PENDING_PAYMENT));
    }

    @Test
    @DisplayName("estimatedCost set from package basePrice")
    void estimatedCostSetFromPackage() {
        Authentication auth = customerAuth();
        when(customerRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(customer));
        when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingResponseDTO result = bookingService.createBooking(buildRequest(), auth);

        assertThat(result.getEstimatedCost()).isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    @DisplayName("Throws when owner subscription is expired")
    void throwsWhenSubscriptionExpired() {
        owner.setSubscriptionStatus("TRIAL_EXPIRED");
        Authentication auth = customerAuth();
        when(customerRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(customer));
        when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> bookingService.createBooking(buildRequest(), auth))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("subscription has expired");
    }

    @Test
    @DisplayName("Manager without customerId throws")
    void managerWithoutCustomerIdThrows() {
        BookingRequestDTO request = buildRequest();
        request.setCustomerId(null);
        Authentication managerAuth = mock(Authentication.class);
        when(managerAuth.getAuthorities()).thenAnswer(inv ->
                List.of(new SimpleGrantedAuthority("ROLE_SERVICE_MANAGER")));

        assertThatThrownBy(() -> bookingService.createBooking(request, managerAuth))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Customer ID is required");
    }

    // ── cancelBooking() ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Booking status set to CANCELLED")
    void cancelSetsStatusCancelled() {
        when(bookingRepository.findById(confirmedBooking.getBookingId()))
                .thenReturn(Optional.of(confirmedBooking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(serviceCenterRepository.findById(centerId)).thenReturn(Optional.of(serviceCenter));

        BookingResponseDTO result = bookingService.cancelBooking(confirmedBooking.getBookingId());

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    @DisplayName("5% penalty applied when cancelling within 3 days")
    void penaltyAppliedWithin3Days() {
        confirmedBooking.setBookingDate(LocalDate.now().plusDays(1));
        when(bookingRepository.findById(confirmedBooking.getBookingId()))
                .thenReturn(Optional.of(confirmedBooking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(serviceCenterRepository.findById(centerId)).thenReturn(Optional.of(serviceCenter));
        when(paymentService.refundPayment(anyString(), anyDouble())).thenReturn(true);

        bookingService.cancelBooking(confirmedBooking.getBookingId());

        verify(paymentService).refundPayment(eq("cs_test_session"), eq(5.0));
    }

    @Test
    @DisplayName("No penalty but full refund when cancelling more than 3 days ahead")
    void noPenaltyBeyond3Days() {
        confirmedBooking.setBookingDate(LocalDate.now().plusDays(10));
        when(bookingRepository.findById(confirmedBooking.getBookingId()))
                .thenReturn(Optional.of(confirmedBooking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(serviceCenterRepository.findById(centerId)).thenReturn(Optional.of(serviceCenter));
        when(paymentService.refundPayment(anyString(), anyDouble())).thenReturn(true);

        bookingService.cancelBooking(confirmedBooking.getBookingId());

        // No penalty, but full refund (0% deduction) is still triggered for paid bookings
        verify(paymentService).refundPayment(eq("cs_test_session"), eq(0.0));
    }

    @Test
    @DisplayName("Returns existing booking when already cancelled")
    void handlesAlreadyCancelled() {
        confirmedBooking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(confirmedBooking.getBookingId()))
                .thenReturn(Optional.of(confirmedBooking));

        BookingResponseDTO result = bookingService.cancelBooking(confirmedBooking.getBookingId());
        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    // ── rescheduleBooking() ──────────────────────────────────────────────────

    @Test
    @DisplayName("Reschedule succeeds when more than 3 days ahead and slot free")
    void rescheduleSuccess() {
        when(bookingRepository.findById(confirmedBooking.getBookingId()))
                .thenReturn(Optional.of(confirmedBooking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(serviceCenterRepository.findById(centerId)).thenReturn(Optional.of(serviceCenter));

        LocalDate newDate = LocalDate.now().plusDays(10);
        LocalTime newTime = LocalTime.of(14, 0);

        BookingResponseDTO result = bookingService.rescheduleBooking(
                confirmedBooking.getBookingId(), newDate, newTime);

        assertThat(result.getBookingDate()).isEqualTo(newDate);
        assertThat(result.getBookingTime()).isEqualTo(newTime);
    }

    @Test
    @DisplayName("Reschedule throws when less than 3 days before booking")
    void throwsWithin3Days() {
        confirmedBooking.setBookingDate(LocalDate.now().plusDays(1));
        when(bookingRepository.findById(confirmedBooking.getBookingId()))
                .thenReturn(Optional.of(confirmedBooking));

        assertThatThrownBy(() -> bookingService.rescheduleBooking(
                confirmedBooking.getBookingId(), LocalDate.now().plusDays(10), LocalTime.of(14, 0)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot reschedule within 3 days");
    }

    @Test
    @DisplayName("Reschedule throws when new slot is already taken")
    void throwsWhenSlotTaken() {
        when(bookingRepository.findById(confirmedBooking.getBookingId()))
                .thenReturn(Optional.of(confirmedBooking));
        when(schedulingService.isSlotAvailable(any(), any(), any(), anyInt(), any())).thenReturn(false);

        assertThatThrownBy(() -> bookingService.rescheduleBooking(
                confirmedBooking.getBookingId(), LocalDate.now().plusDays(10), LocalTime.of(14, 0)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("slot is no longer available");
    }

    // ── completePayment() ────────────────────────────────────────────────────

    @Test
    @DisplayName("completePayment transitions booking to CONFIRMED")
    void completePaymentConfirmsBooking() {
        Booking pending = new Booking();
        pending.setBookingId(UUID.randomUUID());
        pending.setStatus(BookingStatus.PENDING_PAYMENT);
        pending.setCenterId(centerId);

        when(bookingRepository.findById(pending.getBookingId())).thenReturn(Optional.of(pending));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(serviceCenterRepository.findById(centerId)).thenReturn(Optional.of(serviceCenter));

        BookingResponseDTO result = bookingService.completePayment(pending.getBookingId(), "cs_abc");

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(result.getBookingFeePaid()).isTrue();
        assertThat(result.getGatewaySessionId()).isEqualTo("cs_abc");
    }

    // ── service lifecycle ────────────────────────────────────────────────────

    @Test
    @DisplayName("startService sets status to IN_PROGRESS")
    void startServiceTransition() {
        when(bookingRepository.findById(confirmedBooking.getBookingId()))
                .thenReturn(Optional.of(confirmedBooking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(serviceCenterRepository.findById(centerId)).thenReturn(Optional.of(serviceCenter));

        assertThat(bookingService.startService(confirmedBooking.getBookingId()).getStatus())
                .isEqualTo(BookingStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("completeBooking sets status to COMPLETED")
    void completeBookingTransition() {
        when(bookingRepository.findById(confirmedBooking.getBookingId()))
                .thenReturn(Optional.of(confirmedBooking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(serviceCenterRepository.findById(centerId)).thenReturn(Optional.of(serviceCenter));

        assertThat(bookingService.completeBooking(confirmedBooking.getBookingId()).getStatus())
                .isEqualTo(BookingStatus.COMPLETED);
    }

    // ── slot availability ────────────────────────────────────────────────────

    @Test
    @DisplayName("isSlotTaken returns true when slot is taken")
    void slotIsTaken() {
        when(bookingRepository.existsActiveSlot(any(), any(), any(), any())).thenReturn(true);
        assertThat(bookingService.isSlotTaken(centerId, LocalDate.now(), LocalTime.of(10, 0))).isTrue();
    }

    @Test
    @DisplayName("isSlotTaken returns false when slot is free")
    void slotIsFree() {
        when(bookingRepository.existsActiveSlot(any(), any(), any(), any())).thenReturn(false);
        assertThat(bookingService.isSlotTaken(centerId, LocalDate.now(), LocalTime.of(10, 0))).isFalse();
    }

    @Test
    @DisplayName("getAvailableSlots delegates to schedulingService")
    void availableSlotsExcludeTaken() {
        LocalDate date = LocalDate.now().plusDays(1);
        when(schedulingService.getAvailableStartTimes(centerId, date, null))
                .thenReturn(List.of("08:00 AM", "09:15 AM", "10:30 AM", "01:00 PM"));

        List<String> slots = bookingService.getAvailableSlots(centerId, date);

        assertThat(slots).contains("08:00 AM", "09:15 AM");
        assertThat(slots).hasSize(4);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private BookingRequestDTO buildRequest() {
        BookingRequestDTO r = new BookingRequestDTO();
        r.setPackageId(packageId);
        r.setVehicleId(vehicleId);
        r.setCenterId(centerId);
        r.setBookingDate(LocalDate.now().plusDays(7));
        r.setBookingTime(LocalTime.of(10, 0));
        return r;
    }

    private Authentication customerAuth() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("customer@test.com");
        when(auth.getAuthorities()).thenAnswer(inv ->
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        return auth;
    }
}
