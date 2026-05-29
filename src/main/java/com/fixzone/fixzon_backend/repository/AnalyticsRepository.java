package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.Analytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

import java.util.List;

@Repository
public interface AnalyticsRepository extends JpaRepository<Analytics, UUID> {
    List<Analytics> findByServiceCenter_CenterId(UUID centerId);
}
