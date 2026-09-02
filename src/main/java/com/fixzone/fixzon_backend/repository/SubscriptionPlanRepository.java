package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {
    boolean existsByName(String name);
    List<SubscriptionPlan> findByIsActiveTrue();
}
