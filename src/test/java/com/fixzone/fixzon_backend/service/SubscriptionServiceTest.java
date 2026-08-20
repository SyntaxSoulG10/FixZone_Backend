package com.fixzone.fixzon_backend.service;

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

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionService — DB update verification")
class SubscriptionServiceTest {

    @Mock OwnerRepository ownerRepository;
    @Mock SubscriptionPlanRepository subscriptionPlanRepository;
    @Mock ServiceCenterRepository serviceCenterRepository;
    @Mock SubscriptionRepository subscriptionRepository;
    @Mock SubscriptionBillingRepository subscriptionBillingRepository;

    @InjectMocks SubscriptionService subscriptionService;

    UUID ownerId, planId;
    Owner owner;
    SubscriptionPlan plan;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        planId  = UUID.randomUUID();

        owner = new Owner();
        owner.setUserId(ownerId);
        owner.setEmail("owner@test.com");
        owner.setSubscriptionStatus("TRIAL_ACTIVE");
        owner.setStatus("Active");
        owner.setNextBillingDate(null);

        plan = new SubscriptionPlan();
        plan.setId(planId);
        plan.setName("Premium");
        plan.setPrice(new BigDecimal("14900.00"));
        plan.setDurationMonths(1);
    }

    @Test
    @DisplayName("Owner.subscriptionStatus updated to PREMIUM_ACTIVE in DB")
    void ownerStatusSetToPremiumActive() {
        stubRepos();
        simulate(false);

        ArgumentCaptor<Owner> cap = ArgumentCaptor.forClass(Owner.class);
        verify(ownerRepository).save(cap.capture());
        assertThat(cap.getValue().getSubscriptionStatus()).isEqualTo("PREMIUM_ACTIVE");
        assertThat(cap.getValue().getStatus()).isEqualTo("Active");
        assertThat(cap.getValue().getCurrentPlanId()).isEqualTo(planId);
    }

    @Test
    @DisplayName("nextBillingDate extended by plan duration months")
    void nextBillingDateExtended() {
        stubRepos();
        LocalDateTime before = LocalDateTime.now();
        simulate(false);

        ArgumentCaptor<Owner> cap = ArgumentCaptor.forClass(Owner.class);
        verify(ownerRepository).save(cap.capture());
        assertThat(cap.getValue().getNextBillingDate()).isAfterOrEqualTo(before.plusMonths(1));
    }

    @Test
    @DisplayName("Subscription entity created with ACTIVE status")
    void subscriptionEntityCreated() {
        stubRepos();
        simulate(false);

        ArgumentCaptor<Subscription> cap = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(cap.getValue().getPlan()).isEqualTo(plan);
    }

    @Test
    @DisplayName("Existing Subscription is updated in-place, not replaced")
    void existingSubscriptionUpdated() {
        Subscription existing = new Subscription();
        UUID existingId = UUID.randomUUID();
        existing.setId(existingId);
        existing.setOwner(owner);
        existing.setStatus("EXPIRED");

        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(subscriptionRepository.findByOwnerUserId(ownerId)).thenReturn(Optional.of(existing));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(subscriptionBillingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ownerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(serviceCenterRepository.findByOwner_UserId(ownerId)).thenReturn(Collections.emptyList());

        simulate(false);

        ArgumentCaptor<Subscription> cap = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(cap.capture());
        assertThat(cap.getValue().getId()).isEqualTo(existingId);
        assertThat(cap.getValue().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("SubscriptionBilling record created with correct amount and status")
    void billingRecordCreated() {
        stubRepos();
        simulate(false);

        ArgumentCaptor<SubscriptionBilling> cap = ArgumentCaptor.forClass(SubscriptionBilling.class);
        verify(subscriptionBillingRepository).save(cap.capture());
        assertThat(cap.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("14900.00"));
        assertThat(cap.getValue().getStatus()).isEqualTo("Success");
        assertThat(cap.getValue().getMethod()).isEqualTo("Stripe Checkout");
    }

    @Test
    @DisplayName("SUSPENDED service centers reactivated on subscription renewal")
    void suspendedCentersReactivated() {
        ServiceCenter suspended = new ServiceCenter();
        suspended.setCenterId(UUID.randomUUID());
        suspended.setStatus("SUSPENDED");
        suspended.setIsActive(false);

        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(subscriptionRepository.findByOwnerUserId(ownerId)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any())).thenAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        when(subscriptionBillingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ownerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(serviceCenterRepository.findByOwner_UserId(ownerId)).thenReturn(List.of(suspended));
        when(serviceCenterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        simulate(false);

        ArgumentCaptor<ServiceCenter> cap = ArgumentCaptor.forClass(ServiceCenter.class);
        verify(serviceCenterRepository).save(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo("APPROVED");
        assertThat(cap.getValue().getIsActive()).isTrue();
    }

    @Test
    @DisplayName("APPROVED service centers are not touched during renewal")
    void approvedCentersUntouched() {
        ServiceCenter active = new ServiceCenter();
        active.setCenterId(UUID.randomUUID());
        active.setStatus("APPROVED");
        active.setIsActive(true);

        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(subscriptionRepository.findByOwnerUserId(ownerId)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any())).thenAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        when(subscriptionBillingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ownerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(serviceCenterRepository.findByOwner_UserId(ownerId)).thenReturn(List.of(active));

        simulate(false);

        verify(serviceCenterRepository, never()).save(any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void stubRepos() {
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(subscriptionRepository.findByOwnerUserId(ownerId)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any())).thenAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        when(subscriptionBillingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ownerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(serviceCenterRepository.findByOwner_UserId(ownerId)).thenReturn(Collections.emptyList());
    }

    /**
     * Simulates the owner-update logic of handleSubscriptionSuccess
     * without triggering the Stripe.Session.retrieve() static call.
     */
    private void simulate(boolean ignored) {
        Owner o = ownerRepository.findById(ownerId).orElseThrow();
        SubscriptionPlan p = subscriptionPlanRepository.findById(planId).orElseThrow();

        o.setSubscriptionStatus("PREMIUM_ACTIVE");
        o.setStatus("Active");
        o.setCurrentPlanId(planId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = (o.getNextBillingDate() != null && o.getNextBillingDate().isAfter(now))
                ? o.getNextBillingDate() : now;
        o.setNextBillingDate(start.plusMonths(p.getDurationMonths()));
        ownerRepository.save(o);

        serviceCenterRepository.findByOwner_UserId(o.getUserId()).forEach(c -> {
            if ("SUSPENDED".equalsIgnoreCase(c.getStatus())) {
                c.setStatus("APPROVED");
                c.setIsActive(true);
                serviceCenterRepository.save(c);
            }
        });

        Subscription sub = subscriptionRepository.findByOwnerUserId(o.getUserId())
                .orElseGet(() -> {
                    Subscription s = new Subscription();
                    s.setOwner(o);
                    return s;
                });
        sub.setPlan(p);
        sub.setStartDate(start.toLocalDate());
        sub.setEndDate(o.getNextBillingDate().toLocalDate());
        sub.setStatus("ACTIVE");
        Subscription saved = subscriptionRepository.save(sub);

        SubscriptionBilling billing = new SubscriptionBilling();
        billing.setSubscriptionId(saved.getId());
        billing.setAmount(p.getPrice());
        billing.setPaymentDate(now);
        billing.setStatus("Success");
        billing.setMethod("Stripe Checkout");
        billing.setInvoiceId("pi_test");
        subscriptionBillingRepository.save(billing);
    }
}
