package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.SubscriptionBilling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionBillingRepository extends JpaRepository<SubscriptionBilling, UUID> {
    List<SubscriptionBilling> findBySubscriptionIdOrderByPaymentDateDesc(UUID subscriptionId);
}
