package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.ServiceCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceCenterRepository extends JpaRepository<ServiceCenter, UUID> {
       List<ServiceCenter> findByIsActive(Boolean isActive);

       List<ServiceCenter> findByOwner_UserId(UUID userId);

       List<ServiceCenter> findByStatus(String status);

    @Query("SELECT s FROM ServiceCenter s JOIN Owner o ON s.owner.userId = o.userId " +
           "WHERE s.isActive = true AND (s.status IS NULL OR s.status IN ('APPROVED', 'ACTIVE')) " +
           "AND o.stripeOnboardingComplete = true AND o.stripeAccountId IS NOT NULL AND o.stripeAccountId != '' " +
           "AND o.subscriptionStatus NOT IN ('INACTIVE', 'EXPIRED', 'TRIAL_EXPIRED', 'PREMIUM_EXPIRED', 'CANCELLED')")
    Page<ServiceCenter> findActiveAndValidSubscription(Pageable pageable);

    @Query(value = "SELECT s.* FROM service_centers s " +
                   "JOIN owner o ON s.owner_id = o.user_id " +
                   "WHERE s.is_active = true AND (s.status IS NULL OR s.status IN ('APPROVED', 'ACTIVE')) AND s.latitude IS NOT NULL AND s.longitude IS NOT NULL " +
                   "AND o.stripe_onboarding_complete = true AND o.stripe_account_id IS NOT NULL AND o.stripe_account_id != '' " +
                   "AND o.subscription_status NOT IN ('INACTIVE', 'EXPIRED', 'TRIAL_EXPIRED', 'PREMIUM_EXPIRED', 'CANCELLED') " +
                   "AND (6371 * acos(cos(radians(:lat)) * cos(radians(s.latitude)) * cos(radians(s.longitude) - radians(:lng)) + sin(radians(:lat)) * sin(radians(s.latitude)))) <= :radius " +
                   "ORDER BY (6371 * acos(cos(radians(:lat)) * cos(radians(s.latitude)) * cos(radians(s.longitude) - radians(:lng)) + sin(radians(:lat)) * sin(radians(s.latitude)))) ASC",
           countQuery = "SELECT count(*) FROM service_centers s " +
                        "JOIN owner o ON s.owner_id = o.user_id " +
                        "WHERE s.is_active = true AND (s.status IS NULL OR s.status IN ('APPROVED', 'ACTIVE')) AND s.latitude IS NOT NULL AND s.longitude IS NOT NULL " +
                        "AND o.stripe_onboarding_complete = true AND o.stripe_account_id IS NOT NULL AND o.stripe_account_id != '' " +
                        "AND o.subscription_status NOT IN ('INACTIVE', 'EXPIRED', 'TRIAL_EXPIRED', 'PREMIUM_EXPIRED', 'CANCELLED') " +
                        "AND (6371 * acos(cos(radians(:lat)) * cos(radians(s.latitude)) * cos(radians(s.longitude) - radians(:lng)) + sin(radians(:lat)) * sin(radians(s.latitude)))) <= :radius",
           nativeQuery = true)
    Page<ServiceCenter> findNearbyServiceCenters(@Param("lat") Double lat, @Param("lng") Double lng, @Param("radius") Double radius, Pageable pageable);
}
