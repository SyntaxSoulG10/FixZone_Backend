package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.ServiceCenterDTO;
import com.fixzone.fixzon_backend.service.ServiceCenterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import com.fixzone.fixzon_backend.DTO.PagedResponse;

import org.springframework.security.core.context.SecurityContextHolder;
import com.fixzone.fixzon_backend.service.OwnerService;
import com.fixzone.fixzon_backend.DTO.OwnerDTO;
import com.fixzone.fixzon_backend.model.Owner;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/service-centers")
public class ServiceCenterController {

    // Constructor injection strictly enforces dependency presence at instantiation.
    private final ServiceCenterService serviceCenterService;
    private final OwnerService ownerService;
    private final OwnerRepository ownerRepository;

    public ServiceCenterController(ServiceCenterService serviceCenterService, OwnerService ownerService, OwnerRepository ownerRepository) {
        this.serviceCenterService = serviceCenterService;
        this.ownerService = ownerService;
        this.ownerRepository = ownerRepository;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<ServiceCenterDTO>> getAllServiceCenters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(serviceCenterService.getAllServiceCenters(PageRequest.of(page, size)));
    }

    @GetMapping("/nearby")
    public ResponseEntity<?> getNearbyServiceCenters(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "15") Double radius,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (lat < -90 || lat > 90) return ResponseEntity.badRequest().body("Latitude must be between -90 and 90.");
        if (lng < -180 || lng > 180) return ResponseEntity.badRequest().body("Longitude must be between -180 and 180.");
        if (radius <= 0) return ResponseEntity.badRequest().body("Radius must be greater than 0.");
        if (page < 0) return ResponseEntity.badRequest().body("Page cannot be negative.");
        if (size < 1 || size > 100) return ResponseEntity.badRequest().body("Size must be between 1 and 100.");

        PagedResponse<ServiceCenterDTO> response = serviceCenterService.getNearbyServiceCenters(lat, lng, radius, PageRequest.of(page, size));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/current")
    public ResponseEntity<List<ServiceCenterDTO>> getCurrentOwnerCenters() {
        // Get the current authenticated user's email from the SecurityContext
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Retrieve the owner to get their ownerCode
        OwnerDTO owner = ownerService.retrieveOwnerByEmail(email);
        if (owner == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(serviceCenterService.getServiceCentersByOwnerCode(owner.getOwnerCode()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceCenterDTO> getServiceCenterById(@PathVariable UUID id) {
        ServiceCenterDTO center = serviceCenterService.getServiceCenterById(id);
        return center != null ? ResponseEntity.ok(center) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> createServiceCenter(@jakarta.validation.Valid @RequestBody ServiceCenterDTO dto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        
        Owner ownerEntity = ownerRepository.findByEmail(email).orElse(null);
        if (ownerEntity == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Owner not found");
        }

        // 1. Check if approved by SuperAdmin
        if (!"Approved".equalsIgnoreCase(ownerEntity.getStatus()) && !"Active".equalsIgnoreCase(ownerEntity.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Your account must be approved by a SuperAdmin before creating a branch.");
        }

        // 2. Check subscription is active (TRIAL_ACTIVE or PREMIUM_ACTIVE)
        com.fixzone.fixzon_backend.enums.SubscriptionStatus subStatus =
                com.fixzone.fixzon_backend.enums.SubscriptionStatus.fromLegacy(ownerEntity.getSubscriptionStatus());
        if (!subStatus.isAccessAllowed()) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body("Your subscription has expired. Please upgrade your plan before creating a branch.");
        }

        dto.setOwnerId(ownerEntity.getUserId());
        return ResponseEntity.status(201).body(serviceCenterService.createServiceCenter(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceCenterDTO> updateServiceCenter(@PathVariable UUID id,
            @jakarta.validation.Valid @RequestBody ServiceCenterDTO dto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        OwnerDTO owner = ownerService.retrieveOwnerByEmail(email);
        
        ServiceCenterDTO existingCenter = serviceCenterService.getServiceCenterById(id);
        if (existingCenter == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (owner == null || !owner.getUserId().equals(existingCenter.getOwnerId())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Access Denied: You do not own this service center");
        }
        
        ServiceCenterDTO updatedCenter = serviceCenterService.updateServiceCenter(id, dto);
        return updatedCenter != null ? ResponseEntity.ok(updatedCenter) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServiceCenter(@PathVariable UUID id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        OwnerDTO owner = ownerService.retrieveOwnerByEmail(email);
        
        ServiceCenterDTO existingCenter = serviceCenterService.getServiceCenterById(id);
        if (existingCenter == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (owner == null || !owner.getUserId().equals(existingCenter.getOwnerId())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Access Denied: You do not own this service center");
        }

        serviceCenterService.deleteServiceCenter(id);
        return ResponseEntity.noContent().build();
    }
}
