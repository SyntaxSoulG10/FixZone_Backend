package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.enums.SubscriptionStatus;
import com.fixzone.fixzon_backend.model.Owner;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Daily cron job that proactively expires trial and premium subscriptions.
 *
 * Runs at midnight every day. On each run it:
 *   1. Finds owners whose TRIAL_ACTIVE period has passed → sets TRIAL_EXPIRED
 *   2. Finds owners whose PREMIUM_ACTIVE billing date has passed (no auto-renew) → sets PREMIUM_EXPIRED
 *   3. Migrates any legacy status strings ("TRIAL", "ACTIVE", etc.) to new enum values
 *   4. Sends notifications to affected owners
 *
 * The interceptor (SubscriptionInterceptor) still acts as a real-time safety net,
 * but this cron job is the canonical mechanism for status transitions.
 */
@Component
public class SubscriptionScheduler {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionScheduler.class);

    private final OwnerRepository ownerRepository;
    private final NotificationService notificationService;

    public SubscriptionScheduler(OwnerRepository ownerRepository, NotificationService notificationService) {
        this.ownerRepository = ownerRepository;
        this.notificationService = notificationService;
    }

    /**
     * Main scheduled task — runs daily at midnight.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void processSubscriptionExpiries() {
        log.info("=== Subscription Scheduler: Starting daily expiry check ===");

        int trialExpired = expireTrials();
        int premiumExpired = expirePremiums();
        int legacyMigrated = migrateLegacyStatuses();

        log.info("=== Subscription Scheduler: Complete — {} trials expired, {} premiums expired, {} legacy statuses migrated ===",
                trialExpired, premiumExpired, legacyMigrated);
    }

    /**
     * Expire TRIAL_ACTIVE owners whose trialEndsAt date has passed.
     */
    private int expireTrials() {
        LocalDateTime now = LocalDateTime.now();
        List<Owner> expiredTrials = ownerRepository.findBySubscriptionStatusAndTrialEndsAtBefore(
                SubscriptionStatus.TRIAL_ACTIVE.name(), now);

        for (Owner owner : expiredTrials) {
            owner.setSubscriptionStatus(SubscriptionStatus.TRIAL_EXPIRED.name());
            owner.setStatus("Inactive");
            ownerRepository.save(owner);

            log.info("Trial expired for owner: {} ({})", owner.getFullName(), owner.getEmail());

            // Notify the owner
            notifyOwnerSafe(owner,
                    "Free Trial Expired",
                    "Your 30-day free trial has ended. Upgrade to Premium to continue using FixZone and keep your service centers visible to customers.",
                    "WARNING");
        }

        return expiredTrials.size();
    }

    /**
     * Expire PREMIUM_ACTIVE owners whose nextBillingDate has passed and auto-renew is off.
     */
    private int expirePremiums() {
        LocalDateTime now = LocalDateTime.now();
        List<Owner> expiredPremiums = ownerRepository.findBySubscriptionStatusAndNextBillingDateBeforeAndAutoRenewEnabled(
                SubscriptionStatus.PREMIUM_ACTIVE.name(), now, false);

        for (Owner owner : expiredPremiums) {
            owner.setSubscriptionStatus(SubscriptionStatus.PREMIUM_EXPIRED.name());
            owner.setStatus("Inactive");
            ownerRepository.save(owner);

            log.info("Premium expired for owner: {} ({})", owner.getFullName(), owner.getEmail());

            notifyOwnerSafe(owner,
                    "Subscription Expired",
                    "Your Premium subscription has expired. Renew now to restore access and visibility.",
                    "WARNING");
        }

        return expiredPremiums.size();
    }

    /**
     * One-time migration: converts old string statuses to new enum values.
     * After all owners are migrated, this method becomes a no-op.
     * This preserves all existing data — only the status string changes.
     */
    private int migrateLegacyStatuses() {
        List<Owner> allOwners = ownerRepository.findAll();
        int migratedCount = 0;
        LocalDateTime now = LocalDateTime.now();

        for (Owner owner : allOwners) {
            String raw = owner.getSubscriptionStatus();
            if (raw == null) continue;

            // Skip if already a valid new enum value
            try {
                SubscriptionStatus.valueOf(raw);
                continue; // Already migrated
            } catch (IllegalArgumentException e) {
                // Legacy value found — migrate it
            }

            // Map legacy value intelligently based on dates
            String newStatus;
            switch (raw.toUpperCase()) {
                case "TRIAL":
                    // Check if trial has actually expired
                    if (owner.getTrialEndsAt() != null && owner.getTrialEndsAt().isBefore(now)) {
                        newStatus = SubscriptionStatus.TRIAL_EXPIRED.name();
                        owner.setStatus("Inactive");
                    } else {
                        newStatus = SubscriptionStatus.TRIAL_ACTIVE.name();
                        // Keep existing owner.status
                    }
                    break;
                case "ACTIVE":
                    // Check if premium billing has passed
                    if (owner.getNextBillingDate() != null && owner.getNextBillingDate().isBefore(now)) {
                        newStatus = SubscriptionStatus.PREMIUM_EXPIRED.name();
                        owner.setStatus("Inactive");
                    } else {
                        newStatus = SubscriptionStatus.PREMIUM_ACTIVE.name();
                        // Keep existing owner.status
                    }
                    break;
                case "INACTIVE":
                case "EXPIRED":
                    // Determine if it was a trial or premium that expired
                    if (owner.getCurrentPlanId() != null) {
                        newStatus = SubscriptionStatus.PREMIUM_EXPIRED.name();
                    } else {
                        newStatus = SubscriptionStatus.TRIAL_EXPIRED.name();
                    }
                    owner.setStatus("Inactive");
                    break;
                case "SUSPENDED":
                    newStatus = SubscriptionStatus.CANCELLED.name();
                    break;
                default:
                    newStatus = SubscriptionStatus.TRIAL_EXPIRED.name();
                    break;
            }

            owner.setSubscriptionStatus(newStatus);
            ownerRepository.save(owner);
            migratedCount++;
            log.info("Migrated owner {} status: '{}' → '{}'", owner.getEmail(), raw, newStatus);
        }

        return migratedCount;
    }

    /**
     * Safe notification helper that won't crash the scheduler if notification fails.
     */
    private void notifyOwnerSafe(Owner owner, String title, String message, String type) {
        try {
            notificationService.createNotificationSafe(owner, title, message, type,
                    "/dashboard/company-owner/profile?tab=billing");
        } catch (Exception e) {
            log.warn("Failed to send subscription notification to {}: {}", owner.getEmail(), e.getMessage());
        }
    }
}
