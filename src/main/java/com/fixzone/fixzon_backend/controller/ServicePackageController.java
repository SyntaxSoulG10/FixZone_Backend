package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.ServicePackageDTO;
import com.fixzone.fixzon_backend.service.ServicePackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController

@RequestMapping("/api/service-packages")
public class ServicePackageController {

    @Autowired
    private ServicePackageService servicePackageService;

    @GetMapping
    public ResponseEntity<List<ServicePackageDTO>> getAllServicePackages() {
        return ResponseEntity.ok(servicePackageService.getAllServicePackages());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicePackageDTO> getServicePackageById(@PathVariable UUID id) {
        ServicePackageDTO dto = servicePackageService.getServicePackageById(id);
        if (dto != null) {
            return ResponseEntity.ok(dto);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/center/{centerId}")
    public ResponseEntity<List<ServicePackageDTO>> getServicePackagesByCenter(@PathVariable UUID centerId) {
        return ResponseEntity.ok(servicePackageService.getServicePackagesByCenter(centerId));
    }

    @PostMapping
    public ResponseEntity<ServicePackageDTO> createServicePackage(@RequestBody ServicePackageDTO servicePackageDTO) {
        return ResponseEntity.ok(servicePackageService.createServicePackage(servicePackageDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServicePackageDTO> updateServicePackage(@PathVariable UUID id,
            @RequestBody ServicePackageDTO servicePackageDTO) {
        ServicePackageDTO updatedDTO = servicePackageService.updateServicePackage(id, servicePackageDTO);
        if (updatedDTO != null) {
            return ResponseEntity.ok(updatedDTO);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServicePackage(@PathVariable UUID id) {
        if (servicePackageService.deleteServicePackage(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
