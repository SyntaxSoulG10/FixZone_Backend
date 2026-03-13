package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.ServiceCenterDTO;
import com.fixzone.fixzon_backend.service.ServiceCenterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/service-centers")
public class ServiceCenterController {

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
        return ResponseEntity.ok(serviceCenterService.getServiceCenterById(id));
    }

    @PostMapping
    public ResponseEntity<ServiceCenterDTO> createServiceCenter(@RequestBody ServiceCenterDTO dto) {
        return ResponseEntity.ok(serviceCenterService.createServiceCenter(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceCenterDTO> updateServiceCenter(@PathVariable UUID id,
            @RequestBody ServiceCenterDTO dto) {
        return ResponseEntity.ok(serviceCenterService.updateServiceCenter(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServiceCenter(@PathVariable UUID id) {
        serviceCenterService.deleteServiceCenter(id);
        return ResponseEntity.noContent().build();
    }
}
