package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.ServicePackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ServicePackageRepository extends JpaRepository<ServicePackage, UUID> {
}

