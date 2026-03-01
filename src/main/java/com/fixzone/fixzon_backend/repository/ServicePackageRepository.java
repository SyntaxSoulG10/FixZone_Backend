package com.fixzone.fixzon_backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fixzone.fixzon_backend.model.ServicePackage;

@Repository
public interface ServicePackageRepository extends JpaRepository<ServicePackage, UUID> {

}
