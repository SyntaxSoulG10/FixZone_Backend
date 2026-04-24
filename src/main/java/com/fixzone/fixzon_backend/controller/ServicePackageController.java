package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.ServicePackageDTO;
import com.fixzone.fixzon_backend.service.ServicePackageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/service-packages")
@CrossOrigin("*")
public class ServicePackageController {

    private final ServicePackageService service;

    public ServicePackageController(ServicePackageService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ServicePackageDTO>> getAllPackages() {
        return ResponseEntity.ok(service.getAllPackages());
    }

    @GetMapping("/current")
    public ResponseEntity<List<ServicePackageDTO>> getCurrentOwnerPackages() {
        // Hardcoded for development
        return ResponseEntity.ok(service.getPackagesByOwnerCode("FIX001"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicePackageDTO> getPackageById(@PathVariable UUID id) {
        ServicePackageDTO dto = service.getPackageById(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @GetMapping("/center/{centerId}")
    public ResponseEntity<List<ServicePackageDTO>> getPackagesByCenter(@PathVariable UUID centerId) {
        return ResponseEntity.ok(service.getPackagesByCenter(centerId));
    }

    @PostMapping
    public ResponseEntity<ServicePackageDTO> createPackage(@jakarta.validation.Valid @RequestBody ServicePackageDTO dto) {
        try {
            return ResponseEntity.status(201).body(service.createPackage(dto));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create service package: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServicePackageDTO> updatePackage(@PathVariable UUID id, @jakarta.validation.Valid @RequestBody ServicePackageDTO dto) {
        try {
            ServicePackageDTO updated = service.updatePackage(id, dto);
            return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update service package: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePackage(@PathVariable UUID id) {
        try {
            service.deletePackage(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete service package: " + e.getMessage());
        }
    }
}
