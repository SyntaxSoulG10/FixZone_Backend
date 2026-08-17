package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface OwnerRepository extends JpaRepository<Owner, UUID> {
    Optional<Owner> findByOwnerCode(String ownerCode);
    Optional<Owner> findByEmail(String email);
    Optional<Owner> findByEmailIgnoreCase(String email);

    /** Finds owners whose trial period has expired (for cron job). */
    List<Owner> findBySubscriptionStatusAndTrialEndsAtBefore(String status, LocalDateTime date);

    /** Finds owners whose premium billing date has passed (for cron job). */
    List<Owner> findBySubscriptionStatusAndNextBillingDateBefore(String status, LocalDateTime date);

    Optional<Owner> findByStripeCustomerId(String stripeCustomerId);
}
