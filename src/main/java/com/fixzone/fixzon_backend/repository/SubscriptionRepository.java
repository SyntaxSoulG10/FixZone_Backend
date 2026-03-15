package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByOwnerId(Long ownerId);
}
