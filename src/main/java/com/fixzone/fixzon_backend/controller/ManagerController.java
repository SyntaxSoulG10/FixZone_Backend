package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.ManagerDTO;
import com.fixzone.fixzon_backend.service.ManagerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/managers")
@CrossOrigin(origins = "*")
public class ManagerController {

    // Enforcing immutability via constructor-based dependency injection
    // This is superior to @Autowired fields because it makes the controller testable without Spring Context reflection
    private final ManagerService managerService;

    public ManagerController(ManagerService managerService) {
        this.managerService = managerService;
    }

    @GetMapping
    public ResponseEntity<List<ManagerDTO>> getAllManagers() {
        return ResponseEntity.ok(managerService.getAllManagers());
    }

    @GetMapping("/current")
    public ResponseEntity<List<ManagerDTO>> getCurrentOwnerManagers() {
        // Hardcoded for development
        return ResponseEntity.ok(managerService.getManagersByOwnerCode("FIX001"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ManagerDTO> getManagerById(@PathVariable UUID id) {
        ManagerDTO manager = managerService.getManagerById(id);
        // Explicit explicit HTTP 404 Not Found returns gracefully if no data exists
        return manager != null ? ResponseEntity.ok(manager) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<ManagerDTO> createManager(@jakarta.validation.Valid @RequestBody ManagerDTO managerDTO) {
        try {
            ManagerDTO newManager = managerService.createManager(managerDTO);
            return ResponseEntity.status(201).body(newManager);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create manager: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ManagerDTO> updateManager(@PathVariable UUID id, @jakarta.validation.Valid @RequestBody ManagerDTO managerDTO) {
        try {
            ManagerDTO updatedManager = managerService.updateManager(id, managerDTO);
            return updatedManager != null ? ResponseEntity.ok(updatedManager) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update manager: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteManager(@PathVariable UUID id) {
        try {
            managerService.deleteManager(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete manager: " + e.getMessage());
        }
    }
}
