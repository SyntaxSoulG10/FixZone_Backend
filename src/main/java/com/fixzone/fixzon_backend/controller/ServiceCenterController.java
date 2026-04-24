package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.ServiceCenterDTO;
import com.fixzone.fixzon_backend.service.ServiceCenterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/service-centers")
@CrossOrigin("*")
public class ServiceCenterController {

    // Using constructor injection strictly enforces the presence of dependencies at instantiation
    private final ServiceCenterService serviceCenterService;

    public ServiceCenterController(ServiceCenterService serviceCenterService) {
        this.serviceCenterService = serviceCenterService;
    }

    @GetMapping
    public ResponseEntity<List<ServiceCenterDTO>> getAllServiceCenters() {
        return ResponseEntity.ok(serviceCenterService.getAllServiceCenters());
    }

    @GetMapping("/current")
    public ResponseEntity<List<ServiceCenterDTO>> getCurrentOwnerCenters() {
        // Hardcoded for development
        return ResponseEntity.ok(serviceCenterService.getServiceCentersByOwnerCode("FIX001"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceCenterDTO> getServiceCenterById(@PathVariable UUID id) {
        ServiceCenterDTO center = serviceCenterService.getServiceCenterById(id);
        return center != null ? ResponseEntity.ok(center) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<ServiceCenterDTO> createServiceCenter(@jakarta.validation.Valid @RequestBody ServiceCenterDTO dto) {
        try {
            return ResponseEntity.status(201).body(serviceCenterService.createServiceCenter(dto));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create service center: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceCenterDTO> updateServiceCenter(@PathVariable UUID id,
            @jakarta.validation.Valid @RequestBody ServiceCenterDTO dto) {
        try {
            ServiceCenterDTO updatedCenter = serviceCenterService.updateServiceCenter(id, dto);
            return updatedCenter != null ? ResponseEntity.ok(updatedCenter) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update service center: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServiceCenter(@PathVariable UUID id) {
        try {
            serviceCenterService.deleteServiceCenter(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete service center: " + e.getMessage());
        }
    }
}
