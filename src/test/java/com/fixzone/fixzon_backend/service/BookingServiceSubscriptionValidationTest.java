package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.booking.BookingRequestDTO;
import com.fixzone.fixzon_backend.enums.SubscriptionStatus;
import com.fixzone.fixzon_backend.model.*;
import com.fixzone.fixzon_backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceSubscriptionValidationTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private ServiceCenterRepository serviceCenterRepository;
    @Mock private ServicePackageRepository servicePackageRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private PaymentService paymentService;
    @Mock private CustomerRepository customerRepository;
    @Mock private OwnerRepository ownerRepository;

    @InjectMocks
    private BookingService bookingService;

    private UUID packageId;
    private UUID ownerId;
    private Customer customer;
    private ServicePackage servicePackage;
    private ServiceCenter serviceCenter;
    private User ownerUser;

    @BeforeEach
    void setUp() {
        packageId = UUID.randomUUID();
        ownerId = UUID.randomUUID();

        customer = new Customer();
        customer.setUserId(UUID.randomUUID());
        customer.setEmail("customer@test.com");

        ownerUser = new User();
        ownerUser.setUserId(ownerId);

        serviceCenter = new ServiceCenter();
        serviceCenter.setCenterId(UUID.randomUUID());
        serviceCenter.setOwner(ownerUser);

        servicePackage = new ServicePackage();
        servicePackage.setPackageId(packageId);
        servicePackage.setServiceCenter(serviceCenter);
    }

    @Test
    void createBooking_ShouldSucceed_WhenOwnerSubscriptionIsTrialActive() {
        // Arrange
        Owner activeOwner = new Owner();
        activeOwner.setUserId(ownerId);
        activeOwner.setSubscriptionStatus(SubscriptionStatus.TRIAL_ACTIVE.name());

        when(customerRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(customer));
        when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(activeOwner));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingRequestDTO request = new BookingRequestDTO();
        request.setPackageId(packageId);

        // Act & Assert — should NOT throw
        bookingService.createBooking(request, "customer@test.com");
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    void createBooking_ShouldSucceed_WhenOwnerSubscriptionIsPremiumActive() {
        // Arrange
        Owner premiumOwner = new Owner();
        premiumOwner.setUserId(ownerId);
        premiumOwner.setSubscriptionStatus(SubscriptionStatus.PREMIUM_ACTIVE.name());

        when(customerRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(customer));
        when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(premiumOwner));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingRequestDTO request = new BookingRequestDTO();
        request.setPackageId(packageId);

        // Act & Assert — should NOT throw
        bookingService.createBooking(request, "customer@test.com");
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    void createBooking_ShouldThrow_WhenOwnerSubscriptionIsTrialExpired() {
        // Arrange
        Owner expiredOwner = new Owner();
        expiredOwner.setUserId(ownerId);
        expiredOwner.setSubscriptionStatus(SubscriptionStatus.TRIAL_EXPIRED.name());

        when(customerRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(customer));
        when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(expiredOwner));

        BookingRequestDTO request = new BookingRequestDTO();
        request.setPackageId(packageId);

        // Act & Assert
        assertThatThrownBy(() -> bookingService.createBooking(request, "customer@test.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("subscription has expired");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_ShouldThrow_WhenOwnerSubscriptionIsPremiumExpired() {
        // Arrange
        Owner expiredOwner = new Owner();
        expiredOwner.setUserId(ownerId);
        expiredOwner.setSubscriptionStatus(SubscriptionStatus.PREMIUM_EXPIRED.name());

        when(customerRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(customer));
        when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(expiredOwner));

        BookingRequestDTO request = new BookingRequestDTO();
        request.setPackageId(packageId);

        // Act & Assert
        assertThatThrownBy(() -> bookingService.createBooking(request, "customer@test.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("subscription has expired");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_ShouldThrow_WhenOwnerSubscriptionIsCancelled() {
        // Arrange
        Owner cancelledOwner = new Owner();
        cancelledOwner.setUserId(ownerId);
        cancelledOwner.setSubscriptionStatus(SubscriptionStatus.CANCELLED.name());

        when(customerRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(customer));
        when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage));
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(cancelledOwner));

        BookingRequestDTO request = new BookingRequestDTO();
        request.setPackageId(packageId);

        // Act & Assert
        assertThatThrownBy(() -> bookingService.createBooking(request, "customer@test.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("subscription has expired");

        verify(bookingRepository, never()).save(any());
    }
}
