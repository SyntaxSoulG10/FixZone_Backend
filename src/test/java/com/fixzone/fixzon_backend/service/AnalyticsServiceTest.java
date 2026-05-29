package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.AnalyticsDTO;
import com.fixzone.fixzon_backend.enums.BookingStatus;
import com.fixzone.fixzon_backend.model.*;
import com.fixzone.fixzon_backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private ServiceCenterRepository serviceCenterRepository;
    @Mock private ServicePackageRepository servicePackageRepository;
    @Mock private PaymentRecordRepository paymentRecordRepository;
    @Mock private OwnerRepository ownerRepository;
    @Mock private CustomerRepository customerRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private Owner owner;
    private ServiceCenter center;
    private UUID ownerId;
    private UUID centerId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        centerId = UUID.randomUUID();

        owner = new Owner();
        owner.setUserId(ownerId);
        owner.setOwnerCode("COMP-123");

        center = new ServiceCenter();
        center.setCenterId(centerId);
        center.setName("Main Center");
    }

    @Test
    void getCompanyAnalytics_ShouldCalculateCorrectAggregates() {
        // Mock Owner and Center lookup
        when(ownerRepository.findByOwnerCode("COMP-123")).thenReturn(Optional.of(owner));
        when(serviceCenterRepository.findByOwner_UserId(ownerId)).thenReturn(Collections.singletonList(center));

        // Mock Invoices
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(UUID.randomUUID());
        invoice.setCenterId(centerId);
        invoice.setStatus("PAID");
        invoice.setTotal(new BigDecimal("1000.00"));
        invoice.setIssuedAt(LocalDateTime.now());
        when(invoiceRepository.findByCenterIdInAndIssuedAtBetween(anySet(), any(), any()))
                .thenReturn(Collections.singletonList(invoice));

        // Mock Bookings
        Booking booking = new Booking();
        booking.setBookingId(UUID.randomUUID());
        booking.setCenterId(centerId);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookingDate(LocalDateTime.now().toLocalDate());
        when(bookingRepository.findByCenterIdInAndBookingDateBetween(anySet(), any(), any()))
                .thenReturn(Collections.singletonList(booking));

        // Mock Payments
        PaymentRecord payment = new PaymentRecord();
        payment.setPaymentId(UUID.randomUUID());
        payment.setInvoiceId(invoice.getInvoiceId());
        payment.setAmount(new BigDecimal("1000.00"));
        payment.setMethod("CARD");
        payment.setStatus("SUCCESS");
        payment.setCreatedAt(LocalDateTime.now());
        when(paymentRecordRepository.findByCenterIdInAndCreatedAtBetween(anySet(), any(), any()))
                .thenReturn(Collections.singletonList(payment));

        // Mock Packages and Customers (for breakdown and transactions)
        when(servicePackageRepository.findAllById(anySet())).thenReturn(List.of());
        when(customerRepository.findAllById(anySet())).thenReturn(List.of());

        // Execute
        AnalyticsDTO result = analyticsService.getCompanyAnalytics("COMP-123", "all", null, null, "monthly");

        // Verify
        assertThat(result).isNotNull();
        assertThat(result.getTotalRevenue()).isEqualByComparingTo("1000.00");
        assertThat(result.getTotalJobs()).isEqualTo(1);
        assertThat(result.getPendingJobs()).isEqualTo(1);
        assertThat(result.getOnlineRevenue()).isEqualByComparingTo("1000.00");
        assertThat(result.getHandCollectionRevenue()).isEqualByComparingTo("0.00");
    }

    @Test
    void getCompanyAnalytics_WithNoData_ShouldReturnZeroes() {
        when(ownerRepository.findByOwnerCode(anyString())).thenReturn(Optional.empty());
        
        AnalyticsDTO result = analyticsService.getCompanyAnalytics("NONE", "all", null, null, "monthly");

        assertThat(result.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getTotalJobs()).isEqualTo(0);
    }
}
