package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.ManagerDTO;
import com.fixzone.fixzon_backend.service.ManagerService;
import com.fixzone.fixzon_backend.service.OwnerService;
import com.fixzone.fixzon_backend.DTO.OwnerDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/managers")
@Validated // Enables class-level validation
public class ManagerController {

    private static final Logger log = LoggerFactory.getLogger(ManagerController.class);

    private final ManagerService managerService;
    private final OwnerService ownerService;

    public ManagerController(ManagerService managerService, OwnerService ownerService) {
        this.managerService = managerService;
        this.ownerService = ownerService;
    }

    @GetMapping
    public ResponseEntity<List<ManagerDTO>> getAllManagers() {
        log.info("Fetching all managers");
        return ResponseEntity.ok(managerService.getAllManagers());
    }

    /**
     * Retrieves managers specifically assigned to the centers owned by the current user.
     * Used by business owners to manage their local staff without visibility into other companies.
     */
    @GetMapping("/current")
    public ResponseEntity<List<ManagerDTO>> getCurrentOwnerManagers() {
        try {
            String email = getCurrentUserEmail();
            log.info("Fetching managers for owner: {}", email);
            
            OwnerDTO owner = ownerService.retrieveOwnerByEmail(email);
            if (owner == null) {
                log.warn("Owner not found for email: {}", email);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner not found");
            }

            return ResponseEntity.ok(managerService.getManagersByOwnerCode(owner.getOwnerCode()));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch current managers", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch current managers", e);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ManagerDTO> getManagerById(@PathVariable UUID id) {
        log.info("Fetching manager by ID: {}", id);
        ManagerDTO manager = managerService.getManagerById(id);
        if (manager == null) {
            log.warn("Manager not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(manager);
    }

    /**
     * Onboards a new service manager into the platform.
     * This creates a specialized user role capable of overseeing operations at a specific service center.
     */
    @PostMapping
    public ResponseEntity<ManagerDTO> createManager(@Valid @RequestBody ManagerDTO managerDTO) {
        try {
            log.info("Creating new manager: {}", managerDTO.getEmail());
            ManagerDTO newManager = managerService.createManager(managerDTO);
            
            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(newManager.getUserId())
                    .toUri();
            
            return ResponseEntity.created(location).body(newManager);
        } catch (Exception e) {
            log.error("Failed to create manager", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create manager", e);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ManagerDTO> updateManager(@PathVariable UUID id, @Valid @RequestBody ManagerDTO managerDTO) {
        try {
            log.info("Updating manager ID: {}", id);
            ManagerDTO updatedManager = managerService.updateManager(id, managerDTO);
            if (updatedManager == null) {
                log.warn("Manager not found for update with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(updatedManager);
        } catch (Exception e) {
            log.error("Failed to update manager ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update manager", e);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteManager(@PathVariable UUID id) {
        try {
            log.info("Deleting manager ID: {}", id);
            managerService.deleteManager(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            log.warn("Manager not found for deletion with ID: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to delete manager ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete manager", e);
        }
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            return (String) principal;
        }
        
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unknown principal type");
    }
}

