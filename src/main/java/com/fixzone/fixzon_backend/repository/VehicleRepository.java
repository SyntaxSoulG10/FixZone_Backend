package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    List<Vehicle> findByCustomerId(UUID customerId);
}
