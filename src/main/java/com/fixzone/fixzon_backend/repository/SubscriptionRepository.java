package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByOwnerUserId(UUID ownerId);

    long countByStatus(String status);

    java.util.List<Subscription> findByStatus(String status);

    java.util.List<Subscription> findAllByOrderByStartDateDesc();

    long countByStartDateAfter(java.time.LocalDate date);

    long countByStartDateBetween(java.time.LocalDate start, java.time.LocalDate end);
}
