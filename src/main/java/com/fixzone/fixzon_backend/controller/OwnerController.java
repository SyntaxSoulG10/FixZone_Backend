package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.OwnerDTO;
import com.fixzone.fixzon_backend.service.OwnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// We use a dedicated controller to decouple the HTTP routing logic from the business operations.
// This ensures that the frontend communicates with a clean, standardized API surface.
@RestController
@RequestMapping("/api/owners")
@CrossOrigin(origins = "*") // Allows API requests from the frontend regardless of port differences during development
public class OwnerController {

    private final OwnerService ownerService;

    // We inject the service via the constructor instead of field injection.
    // This makes the controller easier to unit test because we can pass mock services directly.
    @Autowired
    public OwnerController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @GetMapping
    public ResponseEntity<List<OwnerDTO>> fetchAllCompanyOwners() {
        // We wrap the list in a ResponseEntity to provide semantic HTTP status codes.
        List<OwnerDTO> ownerList = ownerService.retrieveAllOwners();
        return ResponseEntity.ok(ownerList);
    }

    @GetMapping("/{ownerId}")
    public ResponseEntity<OwnerDTO> fetchOwnerDetails(@PathVariable UUID ownerId) {
        OwnerDTO retrievedOwner = ownerService.retrieveOwnerById(ownerId);
        
        // Return a 404 Not Found if the requested owner doesn't exist, preventing blank responses.
        if (retrievedOwner == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        return ResponseEntity.ok(retrievedOwner);
    }

    @PostMapping
    public ResponseEntity<OwnerDTO> registerNewOwner(@RequestBody OwnerDTO newOwnerData) {
        // Registration is a disruptive action, so we return 201 Created to signify successful creation.
        OwnerDTO createdOwner = ownerService.registerOwner(newOwnerData);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOwner);
    }

    @PutMapping("/{ownerId}")
    public ResponseEntity<OwnerDTO> modifyOwnerDetails(@PathVariable UUID ownerId, @RequestBody OwnerDTO updatedOwnerData) {
        OwnerDTO modifiedOwner = ownerService.modifyOwner(ownerId, updatedOwnerData);
        
        // Similar to the fetch method, we must handle the case where the target ID is invalid.
        if (modifiedOwner == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        return ResponseEntity.ok(modifiedOwner);
    }

    @DeleteMapping("/{ownerId}")
    public ResponseEntity<Void> removeOwnerRecord(@PathVariable UUID ownerId) {
        ownerService.removeOwner(ownerId);
        // We return 204 No Content because the deletion leaves no entity to return in the response body.
        return ResponseEntity.noContent().build();
    }
}
