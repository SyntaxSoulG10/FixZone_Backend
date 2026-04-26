package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.ManagerDTO;
import com.fixzone.fixzon_backend.service.ManagerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import com.fixzone.fixzon_backend.service.OwnerService;
import com.fixzone.fixzon_backend.DTO.OwnerDTO;

@RestController
@RequestMapping("/api/managers")
@CrossOrigin(origins = "*")
public class ManagerController {

    // Enforcing immutability via constructor-based dependency injection
    // This is superior to @Autowired fields because it makes the controller testable without Spring Context reflection
    private final ManagerService managerService;
    private final OwnerService ownerService;

    public ManagerController(ManagerService managerService, OwnerService ownerService) {
        this.managerService = managerService;
        this.ownerService = ownerService;
    }

    @GetMapping
    public ResponseEntity<List<ManagerDTO>> getAllManagers() {
        return ResponseEntity.ok(managerService.getAllManagers());
    }

    @GetMapping("/current")
    public ResponseEntity<List<ManagerDTO>> getCurrentOwnerManagers() {
        try {
            // Get the current authenticated user's email from the SecurityContext
            String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            
            // Retrieve the owner to get their ownerCode
            OwnerDTO owner = ownerService.retrieveOwnerByEmail(email);
            if (owner == null) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).build();
            }

            return ResponseEntity.ok(managerService.getManagersByOwnerCode(owner.getOwnerCode()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch current managers: " + e.getMessage());
        }
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
