package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.entity.Settings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SettingsRepository extends JpaRepository<Settings, Long> {
    Optional<Settings> findByCustomerId(Long customerId);
}
