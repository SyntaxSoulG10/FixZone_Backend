package com.fixzone.fixzon_backend.repository;
 
import com.fixzone.fixzon_backend.model.ServiceCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;
 
public interface ServiceCenterRepository extends JpaRepository<ServiceCenter, UUID> {
    List<ServiceCenter> findByIsActive(Boolean isActive);
}

