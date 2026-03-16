package com.fixzone.fixzon_backend.repository;
 
import com.fixzone.fixzon_backend.entity.ServiceCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
 
public interface ServiceCenterRepository extends JpaRepository<ServiceCenter, Long> {
    List<ServiceCenter> findByStatus(String status);
}
