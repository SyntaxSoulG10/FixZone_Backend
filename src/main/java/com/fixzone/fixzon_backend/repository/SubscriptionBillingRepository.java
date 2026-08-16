package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.SubscriptionBilling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionBillingRepository extends JpaRepository<SubscriptionBilling, UUID> {
    List<SubscriptionBilling> findBySubscriptionIdOrderByPaymentDateDesc(UUID subscriptionId);

    // Daily revenue from subscription payments (last 7 days), grouped by day-of-week
    @Query(value = """
        SELECT EXTRACT(DOW FROM payment_date) as dow, SUM(amount) as daily_total
        FROM subscription_billing
        WHERE payment_date >= :start AND payment_date <= :end
        GROUP BY EXTRACT(DOW FROM payment_date)
        ORDER BY 1
        """, nativeQuery = true)
    List<Object[]> findDailyRevenueBetween(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    // Monthly revenue from subscription payments (last 6 months), grouped by year/month
    @Query(value = """
        SELECT EXTRACT(YEAR FROM payment_date) as year, EXTRACT(MONTH FROM payment_date) as month, SUM(amount) as total
        FROM subscription_billing
        WHERE payment_date >= :start
        GROUP BY EXTRACT(YEAR FROM payment_date), EXTRACT(MONTH FROM payment_date)
        ORDER BY 1, 2
        """, nativeQuery = true)
    List<Object[]> findMonthlyRevenueSince(@Param("start") LocalDateTime start);
}
