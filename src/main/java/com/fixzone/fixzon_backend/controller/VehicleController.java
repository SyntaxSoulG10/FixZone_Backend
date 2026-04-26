package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.VehicleDTO;
import com.fixzone.fixzon_backend.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vehicles")
@CrossOrigin("*")
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    @GetMapping("/user/{customerId}")
    public ResponseEntity<List<VehicleDTO>> getVehiclesByUser(@PathVariable UUID customerId) {
        try {
            return ResponseEntity.ok(vehicleService.getVehiclesByCustomerId(customerId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<VehicleDTO> createVehicle(@RequestBody VehicleDTO vehicleDTO) {
        try {
            return ResponseEntity.ok(vehicleService.createVehicle(vehicleDTO));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create vehicle: " + e.getMessage());
        }
    }

    @PutMapping("/{vehicleId}")
    public ResponseEntity<VehicleDTO> updateVehicle(@PathVariable UUID vehicleId, @RequestBody VehicleDTO vehicleDTO) {
        try {
            return ResponseEntity.ok(vehicleService.updateVehicle(vehicleId, vehicleDTO));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to update vehicle: " + e.getMessage());
        }
    }

    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable UUID vehicleId) {
        try {
            vehicleService.deleteVehicle(vehicleId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to delete vehicle: " + e.getMessage());
        }
    }
}
