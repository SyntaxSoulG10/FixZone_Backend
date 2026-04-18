package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByOwnerUserId(UUID ownerId);
}

