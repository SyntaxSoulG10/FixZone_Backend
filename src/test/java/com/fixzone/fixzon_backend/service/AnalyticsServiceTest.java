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

// Unit tests for AnalyticsService - tests analytics calculations and aggregations using mock repositories
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock private InvoiceRepository invoiceRepository; // Mock to avoid aggregation queries
    @Mock private BookingRepository bookingRepository; // Mock to avoid booking queries
    @Mock private ServiceCenterRepository serviceCenterRepository; // Mock to avoid center queries
    @Mock private ServicePackageRepository servicePackageRepository; // Mock to avoid package lookups
    @Mock private PaymentRecordRepository paymentRecordRepository; // Mock to avoid payment queries
    @Mock private OwnerRepository ownerRepository; // Mock to avoid owner lookups
    @Mock private CustomerRepository customerRepository; // Mock to avoid customer lookups

    @InjectMocks // Inject mocks to test real service calculation logic
    private AnalyticsService analyticsService;

    // Test data used in multiple tests
    private Owner owner;
    private ServiceCenter center;
    private UUID ownerId;
    private UUID centerId;

    // Before each test to ensure isolated test execution
    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID(); // Generate unique owner ID to prevent test collision
        centerId = UUID.randomUUID(); // Generate unique center ID to prevent test collision

        owner = new Owner(); // Create test owner
        owner.setUserId(ownerId);
        owner.setOwnerCode("COMP-123");

        center = new ServiceCenter(); // Create test service center
        center.setCenterId(centerId);
        center.setName("Main Center");
    }

    // Test analytics calculations - critical to verify revenue, payment methods and job counts are accurate
    @Test
    void getCompanyAnalytics_ShouldCalculateCorrectAggregates() {
        when(ownerRepository.findByOwnerCode("COMP-123")).thenReturn(Optional.of(owner)); // Mock to retrieve company owner
        when(serviceCenterRepository.findByOwner_UserId(ownerId)).thenReturn(Collections.singletonList(center)); // Mock to get all company centers
        // Setup mock invoice data to test revenue calculation
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(UUID.randomUUID());
        invoice.setCenterId(centerId);
        invoice.setStatus("PAID");
        invoice.setTotal(new BigDecimal("1000.00"));
        invoice.setIssuedAt(LocalDateTime.now());
        when(invoiceRepository.findByCenterIdInAndIssuedAtBetween(anySet(), any(), any())).thenReturn(Collections.singletonList(invoice)); // Mock to get invoices for period

        // Setup mock booking data to test job counts
        Booking booking = new Booking();
        booking.setBookingId(UUID.randomUUID());
        booking.setCenterId(centerId);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookingDate(LocalDateTime.now().toLocalDate());
        when(bookingRepository.findByCenterIdInAndBookingDateBetween(anySet(), any(), any())).thenReturn(Collections.singletonList(booking)); // Mock to get bookings for period

        // Setup mock payment data to test revenue breakdown by method
        PaymentRecord payment = new PaymentRecord();
        payment.setPaymentId(UUID.randomUUID());
        payment.setInvoiceId(invoice.getInvoiceId());
        payment.setAmount(new BigDecimal("1000.00"));
        payment.setMethod("CARD");
        payment.setStatus("SUCCESS");
        payment.setCreatedAt(LocalDateTime.now());
        when(paymentRecordRepository.findByCenterIdInAndCreatedAtBetween(anySet(), any(), any())).thenReturn(Collections.singletonList(payment)); // Mock to get payment records for breakdown

        when(servicePackageRepository.findAllById(anySet())).thenReturn(List.of()); // Mock package lookups
        when(customerRepository.findAllById(anySet())).thenReturn(List.of()); // Mock customer lookups
        AnalyticsDTO result = analyticsService.getCompanyAnalytics("COMP-123", "all", null, null, "monthly"); // Execute the method under test
        assertThat(result).isNotNull(); // Verify result is enriched with calculations
        assertThat(result.getTotalRevenue()).isEqualByComparingTo("1000.00"); // Verify sum of all invoices
        assertThat(result.getTotalJobs()).isEqualTo(1); // Verify count of bookings
        assertThat(result.getPendingJobs()).isEqualTo(1); // Verify count of unfinished bookings
        assertThat(result.getOnlineRevenue()).isEqualByComparingTo("1000.00"); // Verify card payment amount
        assertThat(result.getHandCollectionRevenue()).isEqualByComparingTo("0.00"); // Verify zero for hand payments
    }

    // Test edge case with missing owner - important to verify system doesn't crash and returns zeros for invalid company code
    @Test
    void getCompanyAnalytics_WithNoData_ShouldReturnZeroes() {
        when(ownerRepository.findByOwnerCode(anyString())).thenReturn(Optional.empty()); // Mock to simulate invalid company code
        AnalyticsDTO result = analyticsService.getCompanyAnalytics("NONE", "all", null, null, "monthly"); // Execute the method under test
        assertThat(result.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO); // Verify returns zero not null
        assertThat(result.getTotalJobs()).isEqualTo(0); // Verify returns zero not null
    }
}
