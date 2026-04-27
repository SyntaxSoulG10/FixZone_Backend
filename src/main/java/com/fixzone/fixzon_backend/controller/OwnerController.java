package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.OwnerDTO;
import com.fixzone.fixzon_backend.service.OwnerService;
//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// We use a dedicated controller to decouple the HTTP routing logic from the business operations.
// This ensures that the frontend communicates with a clean, standardized API surface.
@RestController
@RequestMapping("/api/owners")
@CrossOrigin(origins = "*") // Allows API requests from the frontend regardless of port differences during
                            // development
public class OwnerController {

    private final OwnerService ownerService;

    // We inject the service via the constructor instead of field injection.
    // This makes the controller easier to unit test because we can pass mock
    // services directly.
    public OwnerController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @GetMapping("/current")
    public ResponseEntity<OwnerDTO> fetchCurrentOwner() {
        try {
            // Hardcoded for development until authentication is finished
            OwnerDTO retrievedOwner = ownerService.retrieveOwnerByCode("FIX001");
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
            // We wrap the list in a ResponseEntity to provide semantic HTTP status codes.
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

            // Return a 404 Not Found if the requested owner doesn't exist, preventing blank
            // responses.
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
            // Registration is a disruptive action, so we return 201 Created to signify
            // successful creation.
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

            // Similar to the fetch method, we must handle the case where the target ID is
            // invalid.
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
            // We return 204 No Content because the deletion leaves no entity to return in
            // the response body.
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            throw new RuntimeException("Deletion failed: " + e.getMessage());
        }
    }
}
