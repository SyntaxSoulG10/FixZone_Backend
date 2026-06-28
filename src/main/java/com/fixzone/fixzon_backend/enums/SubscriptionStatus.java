package com.fixzone.fixzon_backend.enums;

/**
 * Canonical subscription statuses for company owners.
 *
 * Status lifecycle:
 *   TRIAL_ACTIVE  → (30 days pass)  → TRIAL_EXPIRED
 *   TRIAL_ACTIVE  → (owner pays)    → PREMIUM_ACTIVE
 *   TRIAL_EXPIRED → (owner pays)    → PREMIUM_ACTIVE
 *   PREMIUM_ACTIVE→ (billing passes)→ PREMIUM_EXPIRED
 *   PREMIUM_ACTIVE→ (owner cancels) → CANCELLED
 *   PREMIUM_EXPIRED / CANCELLED → (owner re-subscribes) → PREMIUM_ACTIVE
 *
 * Backward-compatibility:
 *   Old DB values ("TRIAL", "ACTIVE", "INACTIVE", "EXPIRED") are mapped
 *   to the new enum via {@link #fromLegacy(String)} to avoid data loss
 *   during migration.
 */
public enum SubscriptionStatus {
    TRIAL_ACTIVE,       // Owner registered, within 30-day free trial
    TRIAL_EXPIRED,      // 30-day trial ended, blocked from system
    PREMIUM_ACTIVE,     // Paid Premium subscription is live
    PREMIUM_EXPIRED,    // Premium subscription billing date has passed
    CANCELLED;          // Owner manually cancelled subscription

    /** Returns true if the owner should have full system access. */
    public boolean isAccessAllowed() {
        return this == TRIAL_ACTIVE || this == PREMIUM_ACTIVE;
    }

    /** Returns true if the owner's service centers should be visible to customers. */
    public boolean isVisibleToCustomers() {
        return this == TRIAL_ACTIVE || this == PREMIUM_ACTIVE;
    }

    /**
     * Maps legacy string statuses stored in the DB to the new enum.
     * This ensures zero data loss during the migration period.
     *
     * @param raw the raw status string from the database
     * @return the corresponding SubscriptionStatus enum value
     */
    public static SubscriptionStatus fromLegacy(String raw) {
        if (raw == null) return TRIAL_EXPIRED; // null → treat as expired (safety net)

        // Try direct enum match first (new values)
        try {
            return SubscriptionStatus.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            // Fall through to legacy mapping
        }

        // Map old string values to new enum values
        return switch (raw.toUpperCase()) {
            case "TRIAL"    -> TRIAL_ACTIVE;
            case "ACTIVE"   -> PREMIUM_ACTIVE;
            case "INACTIVE" -> TRIAL_EXPIRED;    // Was set by old interceptor
            case "EXPIRED"  -> PREMIUM_EXPIRED;
            case "SUSPENDED"-> CANCELLED;
            default         -> TRIAL_EXPIRED;    // Unknown → block access (safe default)
        };
    }
}
