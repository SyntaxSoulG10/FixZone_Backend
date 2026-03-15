package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.ServicePackageDTO;
import com.fixzone.fixzon_backend.service.ServicePackageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/services")
@CrossOrigin("*")
public class ServicePackageController {

    private final ServicePackageService servicePackageService;

    public ServicePackageController(ServicePackageService servicePackageService) {
        this.servicePackageService = servicePackageService;
    }

    @GetMapping
    public ResponseEntity<List<ServicePackageDTO>> getAllServicePackages() {
        return ResponseEntity.ok(servicePackageService.getAllServicePackages());
    }
}

