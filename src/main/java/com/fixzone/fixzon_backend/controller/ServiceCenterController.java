package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.ServiceCenterDTO;
import com.fixzone.fixzon_backend.service.ServiceCenterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import com.fixzone.fixzon_backend.service.OwnerService;
import com.fixzone.fixzon_backend.DTO.OwnerDTO;

@RestController
@RequestMapping("/api/service-centers")
@CrossOrigin("*")
public class ServiceCenterController {

    // Using constructor injection strictly enforces the presence of dependencies at instantiation
    private final ServiceCenterService serviceCenterService;
    private final OwnerService ownerService;

    public ServiceCenterController(ServiceCenterService serviceCenterService, OwnerService ownerService) {
        this.serviceCenterService = serviceCenterService;
        this.ownerService = ownerService;
    }

    @GetMapping
    public ResponseEntity<List<ServiceCenterDTO>> getAllServiceCenters() {
        return ResponseEntity.ok(serviceCenterService.getAllServiceCenters());
    }

    @GetMapping("/current")
    public ResponseEntity<List<ServiceCenterDTO>> getCurrentOwnerCenters() {
        try {
            // Get the current authenticated user's email from the SecurityContext
            String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            
            // Retrieve the owner to get their ownerCode
            OwnerDTO owner = ownerService.retrieveOwnerByEmail(email);
            if (owner == null) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).build();
            }

            return ResponseEntity.ok(serviceCenterService.getServiceCentersByOwnerCode(owner.getOwnerCode()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch current centers: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceCenterDTO> getServiceCenterById(@PathVariable UUID id) {
        ServiceCenterDTO center = serviceCenterService.getServiceCenterById(id);
        return center != null ? ResponseEntity.ok(center) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<ServiceCenterDTO> createServiceCenter(@jakarta.validation.Valid @RequestBody ServiceCenterDTO dto) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            OwnerDTO owner = ownerService.retrieveOwnerByEmail(email);
            if (owner != null) {
                dto.setOwnerId(owner.getUserId());
            }
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
