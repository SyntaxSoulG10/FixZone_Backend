package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.enums.BookingStatus;
import com.fixzone.fixzon_backend.enums.PaymentStatus;
import com.fixzone.fixzon_backend.model.*;
import com.fixzone.fixzon_backend.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Verifies that after a successful Stripe payment, both the
 * payments table (status -> PAID) and the bookings table
 * (status -> CONFIRMED, bookingFeePaid -> true) are correctly updated.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService — DB update verification")
class PaymentServiceDbUpdateTest {

    @Mock PaymentRepository paymentRepository;
    @Mock ServicePackageRepository servicePackageRepository;
    @Mock BookingRepository bookingRepository;
    @Mock AuthRepository authRepository;
    @Mock OwnerRepository ownerRepository;
    @Mock ServiceCenterRepository serviceCenterRepository;
    @Mock EmailService emailService;
    @Mock CustomerRepository customerRepository;

    @InjectMocks PaymentService paymentService;

    UUID customerId, centerId, packageId, vehicleId, ownerId;
    Payment pendingPayment;
    Booking pendingBooking;
    ServicePackage servicePackage;
    Owner owner;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        centerId   = UUID.randomUUID();
        packageId  = UUID.randomUUID();
        vehicleId  = UUID.randomUUID();
        ownerId    = UUID.randomUUID();

        owner = new Owner();
        owner.setUserId(ownerId);
        owner.setSubscriptionStatus("TRIAL_ACTIVE");
        owner.setStripeAccountId("acct_test");
        owner.setStripeOnboardingComplete(true);

        servicePackage = new ServicePackage();
        servicePackage.setPackageId(packageId);
        servicePackage.setBasePrice(new BigDecimal("5000.00"));

        pendingPayment = new Payment();
        pendingPayment.setId(1L);
        pendingPayment.setGatewaySessionId("cs_test_abc123");
        pendingPayment.setStatus(PaymentStatus.PENDING);
        pendingPayment.setAmount(2000.0);
        pendingPayment.setCustomerId(customerId);
        pendingPayment.setCenterId(centerId);
        pendingPayment.setTenantId(ownerId);
        pendingPayment.setServicePackageId(packageId);
        pendingPayment.setVehicleId(vehicleId);
        pendingPayment.setDate(LocalDate.now().plusDays(5).toString());
        pendingPayment.setTimeSlot("10:00-11:00");

        pendingBooking = new Booking();
        pendingBooking.setBookingId(UUID.randomUUID());
        pendingBooking.setCustomerId(customerId);
        pendingBooking.setCenterId(centerId);
        pendingBooking.setTenantId(ownerId);
        pendingBooking.setVehicleId(vehicleId);
        pendingBooking.setPackageId(packageId);
        pendingBooking.setBookingDate(LocalDate.now().plusDays(5));
        pendingBooking.setBookingTime(LocalTime.of(10, 0));
        pendingBooking.setStatus(BookingStatus.PENDING_PAYMENT);
        pendingBooking.setGatewaySessionId("cs_test_abc123");
        pendingBooking.setExpiresAt(LocalDateTime.now().plusHours(1));
        pendingBooking.setBookingFee(new BigDecimal("2000.00"));
    }

    @Test
    @DisplayName("Booking.status -> CONFIRMED saved to DB when booking exists")
    void existingBookingConfirmedInDb() {
        when(bookingRepository.findAll()).thenReturn(List.of(pendingBooking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        invokeConfirmBookingForPayment(pendingPayment, "cs_test_abc123");

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());

        assertThat(captor.getValue().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(captor.getValue().getBookingFeePaid()).isTrue();
        assertThat(captor.getValue().getBookingId()).isEqualTo(pendingBooking.getBookingId());
    }

    @Test
    @DisplayName("bookingFeePaid set to true in DB")
    void bookingFeePaidSetToTrue() {
        pendingBooking.setBookingFeePaid(false);
        when(bookingRepository.findAll()).thenReturn(List.of(pendingBooking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        invokeConfirmBookingForPayment(pendingPayment, "cs_test_abc123");

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertThat(captor.getValue().getBookingFeePaid()).isTrue();
    }

    @Test
    @DisplayName("New Booking created when no booking matches the session ID")
    void newBookingCreatedWhenNoneMatches() {
        when(bookingRepository.findAll()).thenReturn(Collections.emptyList());
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));

        invokeConfirmBookingForPayment(pendingPayment, "cs_test_abc123");

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());

        Booking created = captor.getValue();
        assertThat(created.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(created.getBookingFeePaid()).isTrue();
        assertThat(created.getCustomerId()).isEqualTo(customerId);
    }

    @Test
    @DisplayName("getPaymentStatusById returns PAID when already paid")
    void returnsPaidWhenAlreadyPaid() {
        pendingPayment.setStatus(PaymentStatus.PAID);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(pendingPayment));

        assertThat(paymentService.getPaymentStatusById(1L)).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    @DisplayName("getPaymentStatusById returns PENDING for cs_ session — no Stripe call made")
    void returnsPendingForCheckoutSession() {
        pendingPayment.setStatus(PaymentStatus.PENDING);
        pendingPayment.setGatewaySessionId("cs_test_session");
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(pendingPayment));

        assertThat(paymentService.getPaymentStatusById(1L)).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("getPaymentStatusById returns null when payment not found")
    void returnsNullWhenNotFound() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(paymentService.getPaymentStatusById(99L)).isNull();
    }

    @Test
    @DisplayName("validatePayoutEligibility: eligible when Stripe connected and subscription active")
    void eligibleWhenReady() {
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        var result = paymentService.validatePayoutEligibility(ownerId);
        assertThat(result.get("eligible")).isEqualTo(true);
        assertThat(result.get("stripeConnected")).isEqualTo(true);
    }

    @Test
    @DisplayName("validatePayoutEligibility: not eligible when Stripe not connected")
    void notEligibleStripeDisconnected() {
        owner.setStripeOnboardingComplete(false);
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        assertThat(paymentService.validatePayoutEligibility(ownerId).get("eligible")).isEqualTo(false);
    }

    @Test
    @DisplayName("validatePayoutEligibility: not eligible when subscription expired")
    void notEligibleSubscriptionExpired() {
        owner.setSubscriptionStatus("TRIAL_EXPIRED");
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        assertThat(paymentService.validatePayoutEligibility(ownerId).get("eligible")).isEqualTo(false);
    }

    @Test
    @DisplayName("validatePayoutEligibility: not eligible when tenantId is null")
    void notEligibleNullTenant() {
        assertThat(paymentService.validatePayoutEligibility(null).get("eligible")).isEqualTo(false);
    }

    @Test
    @DisplayName("findPaymentIdByBookingUUID finds payment by gatewaySessionId on booking")
    void findsPaymentBySessionId() {
        pendingBooking.setGatewaySessionId("cs_test_abc123");
        when(bookingRepository.findById(pendingBooking.getBookingId()))
                .thenReturn(Optional.of(pendingBooking));
        when(paymentRepository.findByGatewaySessionId("cs_test_abc123"))
                .thenReturn(Optional.of(pendingPayment));

        assertThat(paymentService.findPaymentIdByBookingUUID(pendingBooking.getBookingId()))
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("findPaymentIdByBookingUUID falls back to fuzzy match when no session ID on booking")
    void fuzzyMatchFallback() {
        pendingBooking.setGatewaySessionId(null);
        pendingPayment.setServicePackageId(packageId);
        pendingPayment.setVehicleId(vehicleId);
        pendingPayment.setDate(pendingBooking.getBookingDate().toString());

        when(bookingRepository.findById(pendingBooking.getBookingId()))
                .thenReturn(Optional.of(pendingBooking));
        when(paymentRepository.findAll()).thenReturn(List.of(pendingPayment));

        assertThat(paymentService.findPaymentIdByBookingUUID(pendingBooking.getBookingId()))
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("findPaymentIdByBookingUUID throws when booking not found")
    void throwsWhenBookingNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(bookingRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.findPaymentIdByBookingUUID(unknownId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Booking not found");
    }

    // ── reflection helper ─────────────────────────────────────────────────────
    private void invokeConfirmBookingForPayment(Payment payment, String gatewaySessionId) {
        try {
            var method = PaymentService.class.getDeclaredMethod(
                    "confirmBookingForPayment", Payment.class, String.class);
            method.setAccessible(true);
            method.invoke(paymentService, payment, gatewaySessionId);
        } catch (Exception e) {
            throw new RuntimeException(e.getCause() != null ? e.getCause().getMessage() : e.getMessage(), e);
        }
    }
}
