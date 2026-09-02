package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.InitPaymentRequest;
import com.fixzone.fixzon_backend.model.Payment;
import com.fixzone.fixzon_backend.model.ServicePackage;
import com.fixzone.fixzon_backend.repository.AuthRepository;
import com.fixzone.fixzon_backend.repository.BookingRepository;
import com.fixzone.fixzon_backend.repository.PaymentRepository;
import com.fixzone.fixzon_backend.repository.ServicePackageRepository;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)

class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ServicePackageRepository servicePackageRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private AuthRepository authRepository;
    @Mock
    private OwnerRepository ownerRepository;
    @Mock
    private ServiceCenterRepository serviceCenterRepository;

    @InjectMocks
    private PaymentService paymentService;

    private InitPaymentRequest request;
    private ServicePackage servicePackage;
    private UUID packageId;

    @BeforeEach
    void setUp() {
        packageId = UUID.randomUUID();
        
        request = new InitPaymentRequest();
        request.setServicePackageId(packageId.toString());
        request.setVehicleId(UUID.randomUUID().toString());
        request.setCenterId(UUID.randomUUID().toString());
        request.setDate("2026-05-10");
        request.setTimeSlot("10:00-11:00");

        servicePackage = new ServicePackage();
        servicePackage.setPackageId(packageId);
        servicePackage.setBasePrice(new BigDecimal("1000.00"));
    }

    @Test
    void initPayment_ShouldCalculateCorrectAmountAndSave() {
        // Mocking the repository to return the service package
        when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));
        
        // Mock owner to pass ensurePayoutReady
        com.fixzone.fixzon_backend.model.Owner owner = new com.fixzone.fixzon_backend.model.Owner();
        owner.setUserId(UUID.randomUUID());
        owner.setStripeOnboardingComplete(true);
        owner.setStripeAccountId("acct_123");
        owner.setSubscriptionStatus("PREMIUM_ACTIVE");
        when(ownerRepository.findById(any())).thenReturn(Optional.of(owner));
        
        // Mock service center
        com.fixzone.fixzon_backend.model.ServiceCenter sc = new com.fixzone.fixzon_backend.model.ServiceCenter();
        sc.setOwner(owner);
        when(serviceCenterRepository.findById(any())).thenReturn(Optional.of(sc));
        
        // Capture the saved payment
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.initPayment(request, "BOOK-123", "test@example.com");

        assertThat(result).isNotNull();
        // 40% of 1000.00 should be 400.0
        assertThat(result.getAmount()).isEqualTo(400.0);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void validatePayoutEligibility_ShouldRejectInactiveSubscriptionEvenWhenStripeConnected() {
        com.fixzone.fixzon_backend.model.Owner owner = new com.fixzone.fixzon_backend.model.Owner();
        owner.setUserId(UUID.randomUUID());
        owner.setStripeOnboardingComplete(true);
        owner.setStripeAccountId("acct_123");
        owner.setSubscriptionStatus("INACTIVE");

        when(ownerRepository.findById(owner.getUserId())).thenReturn(Optional.of(owner));

        Map<String, Object> result = paymentService.validatePayoutEligibility(owner.getUserId());

        assertThat(Boolean.TRUE.equals(result.get("eligible"))).isFalse();
        assertThat(result.get("message")).asString().contains("plan or trial is inactive");
    }

    @Test
    void getPaymentStatus_WhenExists_ShouldReturnPayment() {
        Payment payment = new Payment();
        payment.setBookingId(123L);
        
        when(paymentRepository.findAll()).thenReturn(java.util.List.of(payment));

        Payment result = paymentService.getPaymentStatus(123L);

        assertThat(result).isNotNull();
        assertThat(result.getBookingId()).isEqualTo(123L);
    }

    @Test
    void resolveBaseUrl_UsesForwardedHeadersWhenPresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("https");
        request.addHeader("X-Forwarded-Host", "api.fixzone.com");
        request.addHeader("X-Forwarded-Proto", "https");

        String baseUrl = paymentService.resolveBaseUrl(request, null, "http://localhost:8081");

        assertThat(baseUrl).isEqualTo("https://api.fixzone.com");
    }

    @Test
    void resolveBaseUrl_UsesConfiguredValueWhenPresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8081);

        String baseUrl = paymentService.resolveBaseUrl(request, "https://payments.fixzone.com", "http://localhost:8081");

        assertThat(baseUrl).isEqualTo("https://payments.fixzone.com");
    }

    @Test
    void buildConnectErrorMessage_WhenStripeConnectNotEnabled_ShouldReturnFriendlyMessage() {
        String message = paymentService.buildConnectErrorMessage(
                "You can only create new accounts if you've signed up for Connect"
        );

        assertThat(message).contains("Stripe Connect is not enabled");
    }
}

