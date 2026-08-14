package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.enums.SubscriptionStatus;
import com.fixzone.fixzon_backend.model.Owner;
import com.fixzone.fixzon_backend.repository.AuthRepository;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
import com.fixzone.fixzon_backend.repository.SubscriptionBillingRepository;
import com.fixzone.fixzon_backend.repository.SubscriptionPlanRepository;
import com.fixzone.fixzon_backend.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionExpirationTest {

    @Mock private OwnerRepository ownerRepository;
    @Mock private SubscriptionPlanRepository subscriptionPlanRepository;
    @Mock private AuthRepository authRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private SubscriptionBillingRepository subscriptionBillingRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private Owner expiredTrialOwner;
    private Owner expiredPremiumOwner;

    @BeforeEach
    void setUp() {
        // Owner whose 30-day trial has passed
        expiredTrialOwner = new Owner();
        expiredTrialOwner.setEmail("trial@expired.com");
        expiredTrialOwner.setSubscriptionStatus(SubscriptionStatus.TRIAL_ACTIVE.name());
        expiredTrialOwner.setTrialEndsAt(LocalDateTime.now().minusDays(1));

        // Premium owner with no auto-renew whose billing date has passed
        expiredPremiumOwner = new Owner();
        expiredPremiumOwner.setEmail("premium@expired.com");
        expiredPremiumOwner.setSubscriptionStatus(SubscriptionStatus.PREMIUM_ACTIVE.name());
        expiredPremiumOwner.setNextBillingDate(LocalDateTime.now().minusDays(1));
        expiredPremiumOwner.setAutoRenewEnabled(false);
    }

    @Test
    void checkSubscriptionExpirations_ShouldSetTrialExpired_WhenTrialEndsAtHasPassed() {
        // Arrange
        when(ownerRepository.findBySubscriptionStatusAndTrialEndsAtBefore(
                eq(SubscriptionStatus.TRIAL_ACTIVE.name()), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(expiredTrialOwner));
        when(ownerRepository.findBySubscriptionStatusAndNextBillingDateBeforeAndAutoRenewEnabled(
                anyString(), any(LocalDateTime.class), eq(false)))
                .thenReturn(List.of());

        // Act
        subscriptionService.checkSubscriptionExpirations();

        // Assert
        assertEquals(SubscriptionStatus.TRIAL_EXPIRED.name(), expiredTrialOwner.getSubscriptionStatus());
        verify(ownerRepository, times(1)).save(expiredTrialOwner);
    }

    @Test
    void checkSubscriptionExpirations_ShouldSetPremiumExpired_WhenBillingDateHasPassed() {
        // Arrange
        when(ownerRepository.findBySubscriptionStatusAndTrialEndsAtBefore(
                anyString(), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(ownerRepository.findBySubscriptionStatusAndNextBillingDateBeforeAndAutoRenewEnabled(
                eq(SubscriptionStatus.PREMIUM_ACTIVE.name()), any(LocalDateTime.class), eq(false)))
                .thenReturn(Arrays.asList(expiredPremiumOwner));

        // Act
        subscriptionService.checkSubscriptionExpirations();

        // Assert
        assertEquals(SubscriptionStatus.PREMIUM_EXPIRED.name(), expiredPremiumOwner.getSubscriptionStatus());
        verify(ownerRepository, times(1)).save(expiredPremiumOwner);
    }

    @Test
    void checkSubscriptionExpirations_ShouldDoNothing_WhenNoExpiredOwners() {
        // Arrange
        when(ownerRepository.findBySubscriptionStatusAndTrialEndsAtBefore(anyString(), any()))
                .thenReturn(List.of());
        when(ownerRepository.findBySubscriptionStatusAndNextBillingDateBeforeAndAutoRenewEnabled(anyString(), any(), eq(false)))
                .thenReturn(List.of());

        // Act
        subscriptionService.checkSubscriptionExpirations();

        // Assert — nothing should be saved
        verify(ownerRepository, never()).save(any());
    }

    @Test
    void checkSubscriptionExpirations_ShouldExpireMultipleTrialOwners() {
        // Arrange
        Owner second = new Owner();
        second.setEmail("second@expired.com");
        second.setSubscriptionStatus(SubscriptionStatus.TRIAL_ACTIVE.name());
        second.setTrialEndsAt(LocalDateTime.now().minusDays(5));

        when(ownerRepository.findBySubscriptionStatusAndTrialEndsAtBefore(
                eq(SubscriptionStatus.TRIAL_ACTIVE.name()), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(expiredTrialOwner, second));
        when(ownerRepository.findBySubscriptionStatusAndNextBillingDateBeforeAndAutoRenewEnabled(
                anyString(), any(), eq(false)))
                .thenReturn(List.of());

        // Act
        subscriptionService.checkSubscriptionExpirations();

        // Assert both are expired
        assertEquals(SubscriptionStatus.TRIAL_EXPIRED.name(), expiredTrialOwner.getSubscriptionStatus());
        assertEquals(SubscriptionStatus.TRIAL_EXPIRED.name(), second.getSubscriptionStatus());
        verify(ownerRepository, times(2)).save(any(Owner.class));
    }
}
