package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.Manager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.Optional;
import java.util.List;

@Repository
public interface ManagerRepository extends JpaRepository<Manager, UUID> {
    Optional<Manager> findByManagerCode(String managerCode);
    List<Manager> findByManagedCenterIdIn(List<UUID> centerIds);
    Optional<Manager> findByManagedCenterId(UUID managedCenterId);
}
