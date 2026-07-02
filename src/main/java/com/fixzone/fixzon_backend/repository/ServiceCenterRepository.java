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

    @Query("SELECT s FROM ServiceCenter s LEFT JOIN Owner o ON s.owner.userId = o.userId " +
           "WHERE s.isActive = true AND (s.owner IS NULL OR o IS NULL OR (o.subscriptionStatus != 'INACTIVE' AND o.subscriptionStatus != 'EXPIRED'))")
    Page<ServiceCenter> findActiveAndValidSubscription(Pageable pageable);

    @Query(value = "SELECT s.* FROM service_centers s " +
                   "LEFT JOIN owner o ON s.owner_id = o.user_id " +
                   "WHERE s.is_active = true AND s.latitude IS NOT NULL AND s.longitude IS NOT NULL " +
                   "AND (s.owner_id IS NULL OR o.subscription_status NOT IN ('INACTIVE', 'EXPIRED')) " +
                   "AND (6371 * acos(cos(radians(:lat)) * cos(radians(s.latitude)) * cos(radians(s.longitude) - radians(:lng)) + sin(radians(:lat)) * sin(radians(s.latitude)))) <= :radius " +
                   "ORDER BY (6371 * acos(cos(radians(:lat)) * cos(radians(s.latitude)) * cos(radians(s.longitude) - radians(:lng)) + sin(radians(:lat)) * sin(radians(s.latitude)))) ASC",
           countQuery = "SELECT count(*) FROM service_centers s " +
                        "LEFT JOIN owner o ON s.owner_id = o.user_id " +
                        "WHERE s.is_active = true AND s.latitude IS NOT NULL AND s.longitude IS NOT NULL " +
                        "AND (s.owner_id IS NULL OR o.subscription_status NOT IN ('INACTIVE', 'EXPIRED')) " +
                        "AND (6371 * acos(cos(radians(:lat)) * cos(radians(s.latitude)) * cos(radians(s.longitude) - radians(:lng)) + sin(radians(:lat)) * sin(radians(s.latitude)))) <= :radius",
           nativeQuery = true)
    Page<ServiceCenter> findNearbyServiceCenters(@Param("lat") Double lat, @Param("lng") Double lng, @Param("radius") Double radius, Pageable pageable);
}

