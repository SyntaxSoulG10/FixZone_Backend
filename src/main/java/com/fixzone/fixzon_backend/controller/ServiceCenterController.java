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

    @GetMapping("/{id}")
    public ResponseEntity<ServiceCenterDTO> getServiceCenterById(@PathVariable UUID id) {
        ServiceCenterDTO center = serviceCenterService.getServiceCenterById(id);
        return center != null ? ResponseEntity.ok(center) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<ServiceCenterDTO> createServiceCenter(@RequestBody ServiceCenterDTO dto) {
        // Semantic 201 Created explicitly shows that a new resource has been allocated
        return ResponseEntity.status(201).body(serviceCenterService.createServiceCenter(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceCenterDTO> updateServiceCenter(@PathVariable UUID id,
            @RequestBody ServiceCenterDTO dto) {
        ServiceCenterDTO updatedCenter = serviceCenterService.updateServiceCenter(id, dto);
        return updatedCenter != null ? ResponseEntity.ok(updatedCenter) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServiceCenter(@PathVariable UUID id) {
        serviceCenterService.deleteServiceCenter(id);
        // Explicit semantic 204 informs the frontend without wasting bandwidth
        return ResponseEntity.noContent().build();
    }
}
