package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByOwnerUserId(UUID ownerId);

    long countByStatus(String status);

    List<Subscription> findByStatus(String status);

    List<Subscription> findAllByOrderByStartDateDesc();

    long countByStartDateAfter(LocalDate date);

    long countByStartDateBetween(LocalDate start, LocalDate end);

    // Count new subscribers per day-of-week (for weekly trend chart)
    @Query(value = """
        SELECT EXTRACT(DOW FROM start_date) as dow, COUNT(*) as subscriber_count
        FROM subscriptions
        WHERE start_date >= :start AND start_date <= :end
        GROUP BY EXTRACT(DOW FROM start_date)
        ORDER BY 1
        """, nativeQuery = true)
    List<Object[]> countNewSubscribersPerDayOfWeek(
        @Param("start") LocalDate start,
        @Param("end") LocalDate end
    );

    // Count new subscribers per month (for monthly trend chart)
    @Query(value = """
        SELECT EXTRACT(YEAR FROM start_date) as year, EXTRACT(MONTH FROM start_date) as month, COUNT(*) as subscriber_count
        FROM subscriptions
        WHERE start_date >= :start
        GROUP BY EXTRACT(YEAR FROM start_date), EXTRACT(MONTH FROM start_date)
        ORDER BY 1, 2
        """, nativeQuery = true)
    List<Object[]> countNewSubscribersPerMonth(@Param("start") LocalDate start);
}
