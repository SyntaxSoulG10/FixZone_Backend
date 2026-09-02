package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.Analytics;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

import java.util.List;

public interface AnalyticsRepository extends JpaRepository<Analytics, UUID> {
    List<Analytics> findByServiceCenter_CenterId(UUID centerId);
}
