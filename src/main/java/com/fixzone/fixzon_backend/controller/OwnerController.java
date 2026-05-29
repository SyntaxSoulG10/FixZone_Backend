package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.OwnerDTO;
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
        // Retrieve current authenticated user's email from SecurityContext
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

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

    @PutMapping("/{ownerId}")
    public ResponseEntity<OwnerDTO> modifyOwnerDetails(@PathVariable UUID ownerId,
            @jakarta.validation.Valid @RequestBody OwnerDTO updatedOwnerData) {
        OwnerDTO modifiedOwner = ownerService.modifyOwner(ownerId, updatedOwnerData);

        // Returns 404 Not Found if the updated owner does not exist
        if (modifiedOwner == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(modifiedOwner);
    }

    @DeleteMapping("/{ownerId}")
    public ResponseEntity<Void> removeOwnerRecord(@PathVariable UUID ownerId) {
        ownerService.removeOwner(ownerId);
        // Returns 204 No Content for successful deletion
        return ResponseEntity.noContent().build();
    }
}
