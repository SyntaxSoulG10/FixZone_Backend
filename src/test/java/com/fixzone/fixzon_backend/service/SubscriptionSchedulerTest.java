package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.enums.SubscriptionStatus;
import com.fixzone.fixzon_backend.model.Owner;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
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
@SuppressWarnings("null")
public class SubscriptionSchedulerTest {

    @Mock
    private OwnerRepository ownerRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SubscriptionScheduler scheduler;

    private Owner activeTrialOwner;
    private Owner expiredTrialOwner;
    private Owner activePremiumOwner;
    private Owner expiredPremiumOwner;

    @BeforeEach
    void setUp() {
        activeTrialOwner = new Owner();
        activeTrialOwner.setEmail("active@trial.com");
        activeTrialOwner.setSubscriptionStatus(SubscriptionStatus.TRIAL_ACTIVE.name());
        activeTrialOwner.setTrialEndsAt(LocalDateTime.now().plusDays(5));

        expiredTrialOwner = new Owner();
        expiredTrialOwner.setEmail("expired@trial.com");
        expiredTrialOwner.setSubscriptionStatus(SubscriptionStatus.TRIAL_ACTIVE.name());
        expiredTrialOwner.setTrialEndsAt(LocalDateTime.now().minusDays(1));

        activePremiumOwner = new Owner();
        activePremiumOwner.setEmail("active@premium.com");
        activePremiumOwner.setSubscriptionStatus(SubscriptionStatus.PREMIUM_ACTIVE.name());
        activePremiumOwner.setNextBillingDate(LocalDateTime.now().plusDays(10));
        activePremiumOwner.setAutoRenewEnabled(false);

        expiredPremiumOwner = new Owner();
        expiredPremiumOwner.setEmail("expired@premium.com");
        expiredPremiumOwner.setSubscriptionStatus(SubscriptionStatus.PREMIUM_ACTIVE.name());
        expiredPremiumOwner.setNextBillingDate(LocalDateTime.now().minusDays(1));
        expiredPremiumOwner.setAutoRenewEnabled(false);
    }

    @Test
    void processSubscriptionExpiries_ShouldExpireTrials() {
        // Arrange
        when(ownerRepository.findBySubscriptionStatusAndTrialEndsAtBefore(
                eq(SubscriptionStatus.TRIAL_ACTIVE.name()), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(expiredTrialOwner));
                
        when(ownerRepository.findBySubscriptionStatusAndNextBillingDateBeforeAndAutoRenewEnabled(
                anyString(), any(LocalDateTime.class), eq(false)))
                .thenReturn(List.of());

        // Act
        scheduler.processSubscriptionExpiries();

        // Assert
        assertEquals(SubscriptionStatus.TRIAL_EXPIRED.name(), expiredTrialOwner.getSubscriptionStatus());
        assertEquals("Inactive", expiredTrialOwner.getStatus());
        verify(ownerRepository, times(1)).save(expiredTrialOwner);
        verify(notificationService, times(1)).createNotificationSafe(
                eq(expiredTrialOwner), anyString(), anyString(), eq("WARNING"), anyString());
    }

    @Test
    void processSubscriptionExpiries_ShouldExpirePremiums() {
        // Arrange
        when(ownerRepository.findBySubscriptionStatusAndTrialEndsAtBefore(
                anyString(), any(LocalDateTime.class)))
                .thenReturn(List.of());
                
        when(ownerRepository.findBySubscriptionStatusAndNextBillingDateBeforeAndAutoRenewEnabled(
                eq(SubscriptionStatus.PREMIUM_ACTIVE.name()), any(LocalDateTime.class), eq(false)))
                .thenReturn(Arrays.asList(expiredPremiumOwner));

        // Act
        scheduler.processSubscriptionExpiries();

        // Assert
        assertEquals(SubscriptionStatus.PREMIUM_EXPIRED.name(), expiredPremiumOwner.getSubscriptionStatus());
        assertEquals("Inactive", expiredPremiumOwner.getStatus());
        verify(ownerRepository, times(1)).save(expiredPremiumOwner);
        verify(notificationService, times(1)).createNotificationSafe(
                eq(expiredPremiumOwner), anyString(), anyString(), eq("WARNING"), anyString());
    }
}
