package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.ServicePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ServicePackageRepository extends JpaRepository<ServicePackage, UUID> {
    List<ServicePackage> findByServiceCenter_CenterId(UUID centerId);
    List<ServicePackage> findByServiceCenter_CenterIdAndIsActiveTrue(UUID centerId);
    List<ServicePackage> findByServiceCenter_CenterIdInAndIsActiveTrue(java.util.Collection<UUID> centerIds);
    List<ServicePackage> findByIsActiveTrue();

    @org.springframework.data.jpa.repository.Query("SELECT sp FROM ServicePackage sp JOIN sp.serviceCenter sc JOIN User u ON sc.owner.userId = u.userId WHERE u.ownerCode = :ownerCode AND sp.isActive = true")
    List<ServicePackage> findPackagesByOwnerCode(@org.springframework.data.repository.query.Param("ownerCode") String ownerCode);

    @org.springframework.data.jpa.repository.Query("SELECT sp FROM ServicePackage sp JOIN sp.serviceCenter sc JOIN User u ON sc.owner.userId = u.userId WHERE u.email = :email AND sp.isActive = true")
    List<ServicePackage> findPackagesByOwnerEmail(@org.springframework.data.repository.query.Param("email") String email);
}
