package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.ServicePackageDTO;
import com.fixzone.fixzon_backend.service.ServicePackageService;
import com.fixzone.fixzon_backend.service.OwnerService;
import com.fixzone.fixzon_backend.service.ServiceCenterService;
import com.fixzone.fixzon_backend.DTO.OwnerDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/service-packages")
public class ServicePackageController {

    private final ServicePackageService service;
    private final OwnerService ownerService;
    private final ServiceCenterService serviceCenterService;

    public ServicePackageController(ServicePackageService service, OwnerService ownerService, ServiceCenterService serviceCenterService) {
        this.service = service;
        this.ownerService = ownerService;
        this.serviceCenterService = serviceCenterService;
    }

    @GetMapping
    public ResponseEntity<List<ServicePackageDTO>> getAllPackages() {
        return ResponseEntity.ok(service.getAllPackages());
    }

    @GetMapping("/current")
    public ResponseEntity<List<ServicePackageDTO>> getCurrentOwnerPackages() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(service.getPackagesByOwnerEmail(email));
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
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        OwnerDTO owner = ownerService.retrieveOwnerByEmail(email);
        
        if (owner != null && dto.getCenterId() != null) {
            boolean ownsCenter = serviceCenterService.getServiceCentersByOwnerCode(owner.getOwnerCode())
                .stream().anyMatch(c -> c.getCenterId().equals(dto.getCenterId()));
            if (!ownsCenter) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You do not own this service center");
            }
        }
        return ResponseEntity.status(201).body(service.createPackage(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServicePackageDTO> updatePackage(@PathVariable UUID id, @jakarta.validation.Valid @RequestBody ServicePackageDTO dto) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        OwnerDTO owner = ownerService.retrieveOwnerByEmail(email);
        
        ServicePackageDTO existing = service.getPackageById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (owner != null) {
            boolean ownsCenter = serviceCenterService.getServiceCentersByOwnerCode(owner.getOwnerCode())
                .stream().anyMatch(c -> c.getCenterId().equals(existing.getCenterId()));
            if (!ownsCenter) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You do not own this service package");
            }
            if (dto.getCenterId() != null && !dto.getCenterId().equals(existing.getCenterId())) {
                boolean ownsNewCenter = serviceCenterService.getServiceCentersByOwnerCode(owner.getOwnerCode())
                    .stream().anyMatch(c -> c.getCenterId().equals(dto.getCenterId()));
                if (!ownsNewCenter) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You do not own the target service center");
                }
            }
        }
        
        ServicePackageDTO updated = service.updatePackage(id, dto);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePackage(@PathVariable UUID id) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        OwnerDTO owner = ownerService.retrieveOwnerByEmail(email);
        
        ServicePackageDTO existing = service.getPackageById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (owner != null) {
            boolean ownsCenter = serviceCenterService.getServiceCentersByOwnerCode(owner.getOwnerCode())
                .stream().anyMatch(c -> c.getCenterId().equals(existing.getCenterId()));
            if (!ownsCenter) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You do not own this service package");
            }
        }
        
        service.deletePackage(id);
        return ResponseEntity.noContent().build();
    }
}
