package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.OwnerDTO;
import com.fixzone.fixzon_backend.middleware.RequireRole;
import com.fixzone.fixzon_backend.service.OwnerService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.UUID;

// Dedicated controller to decouple HTTP routing from business operations.
@RestController
@RequestMapping("/api/owners")
@RequireRole({"ROLE_COMPANY_OWNER", "ROLE_SUPER_ADMIN"})
public class OwnerController {

    private final OwnerService ownerService;

    // Constructor injection for dependency management to improve testability.
    public OwnerController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    /**
     * Retrieves the profile data for the currently authenticated owner.
     * This is used to populate the owner dashboard with company-specific details (e.g., company name).
     */
    @GetMapping("/current")
    public ResponseEntity<OwnerDTO> fetchCurrentOwner() {
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String email = auth.getName();

        OwnerDTO retrievedOwner = ownerService.retrieveOwnerByEmail(email);
        if (retrievedOwner == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(retrievedOwner);
    }

    /**
     * Aggregates all registered company owners for system-wide management.
     * Allows Super Admins to monitor business registration and platform growth.
     */
    @GetMapping
    @RequireRole({"ROLE_SUPER_ADMIN"})
    public ResponseEntity<List<OwnerDTO>> fetchAllCompanyOwners() {
        // Wraps response in ResponseEntity to provide proper HTTP status
        List<OwnerDTO> ownerList = ownerService.retrieveAllOwners();
        return ResponseEntity.ok(ownerList);
    }

    @GetMapping("/{ownerId}")
    public ResponseEntity<OwnerDTO> fetchOwnerDetails(@PathVariable UUID ownerId) {
        OwnerDTO retrievedOwner = ownerService.retrieveOwnerById(ownerId);

        // Handles non-existent owner requests with 404 Not Found
        if (retrievedOwner == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(retrievedOwner);
    }

    @PostMapping
    public ResponseEntity<OwnerDTO> registerNewOwner(@jakarta.validation.Valid @RequestBody OwnerDTO newOwnerData) {
        // Returns 201 Created upon successful registration
        OwnerDTO createdOwner = ownerService.registerOwner(newOwnerData);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOwner);
    }

    @PutMapping("/current")
    public ResponseEntity<OwnerDTO> modifyCurrentOwnerDetails(
            @RequestBody OwnerDTO updatedOwnerData) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        OwnerDTO currentOwner = ownerService.retrieveOwnerByEmail(email);

        if (currentOwner == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        OwnerDTO modifiedOwner = ownerService.modifyOwner(currentOwner.getUserId(), updatedOwnerData);
        if (modifiedOwner == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(modifiedOwner);
    }

    @PutMapping("/{ownerId}")
    public ResponseEntity<OwnerDTO> modifyOwnerDetails(@PathVariable UUID ownerId,
            @RequestBody OwnerDTO updatedOwnerData) {
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN") || a.getAuthority().equals("SUPER_ADMIN"));

        if (!isSuperAdmin) {
            String email = (String) auth.getPrincipal();
            OwnerDTO currentOwner = ownerService.retrieveOwnerByEmail(email);

            if (currentOwner == null || !currentOwner.getUserId().equals(ownerId)) {
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You cannot modify another owner's profile");
            }
        }

        OwnerDTO modifiedOwner = ownerService.modifyOwner(ownerId, updatedOwnerData);

        if (modifiedOwner == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(modifiedOwner);
    }

    @DeleteMapping("/current")
    public ResponseEntity<Void> removeCurrentOwner() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        OwnerDTO currentOwner = ownerService.retrieveOwnerByEmail(email);

        if (currentOwner == null) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Owner profile not found");
        }

        ownerService.removeOwner(currentOwner.getUserId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{ownerId}")
    public ResponseEntity<Void> removeOwnerRecord(@PathVariable UUID ownerId) {
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN") || a.getAuthority().equals("SUPER_ADMIN"));

        if (!isSuperAdmin) {
            String email = (String) auth.getPrincipal();
            OwnerDTO currentOwner = ownerService.retrieveOwnerByEmail(email);

            if (currentOwner == null || !currentOwner.getUserId().equals(ownerId)) {
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You cannot delete another owner's profile");
            }
        }

        ownerService.removeOwner(ownerId);
        return ResponseEntity.noContent().build();
    }
}
