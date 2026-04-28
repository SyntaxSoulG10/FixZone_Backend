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
@CrossOrigin(origins = "*") // Enables Cross-Origin Resource Sharing for API requests
public class OwnerController {

    private final OwnerService ownerService;

    // Constructor injection for dependency management to improve testability.
    public OwnerController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @GetMapping("/current")
    public ResponseEntity<OwnerDTO> fetchCurrentOwner() {
        try {
            // Retrieve current authenticated user's email from SecurityContext
            String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            
            OwnerDTO retrievedOwner = ownerService.retrieveOwnerByEmail(email);
            if (retrievedOwner == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.ok(retrievedOwner);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch current owner: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<OwnerDTO>> fetchAllCompanyOwners() {
        try {
            // Wraps response in ResponseEntity to provide proper HTTP status
            List<OwnerDTO> ownerList = ownerService.retrieveAllOwners();
            return ResponseEntity.ok(ownerList);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch owners: " + e.getMessage());
        }
    }

    @GetMapping("/{ownerId}")
    public ResponseEntity<OwnerDTO> fetchOwnerDetails(@PathVariable UUID ownerId) {
        try {
            OwnerDTO retrievedOwner = ownerService.retrieveOwnerById(ownerId);

            // Handles non-existent owner requests with 404 Not Found
            if (retrievedOwner == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            return ResponseEntity.ok(retrievedOwner);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch owner details: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<OwnerDTO> registerNewOwner(@jakarta.validation.Valid @RequestBody OwnerDTO newOwnerData) {
        try {
            // Returns 201 Created upon successful registration
            OwnerDTO createdOwner = ownerService.registerOwner(newOwnerData);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdOwner);
        } catch (Exception e) {
            throw new RuntimeException("Registration failed: " + e.getMessage());
        }
    }

    @PutMapping("/{ownerId}")
    public ResponseEntity<OwnerDTO> modifyOwnerDetails(@PathVariable UUID ownerId,
            @jakarta.validation.Valid @RequestBody OwnerDTO updatedOwnerData) {
        try {
            OwnerDTO modifiedOwner = ownerService.modifyOwner(ownerId, updatedOwnerData);

            // Returns 404 Not Found if the updated owner does not exist
            if (modifiedOwner == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            return ResponseEntity.ok(modifiedOwner);
        } catch (Exception e) {
            throw new RuntimeException("Update failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/{ownerId}")
    public ResponseEntity<Void> removeOwnerRecord(@PathVariable UUID ownerId) {
        try {
            ownerService.removeOwner(ownerId);
            // Returns 204 No Content for successful deletion
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            throw new RuntimeException("Deletion failed: " + e.getMessage());
        }
    }
}
