package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.ManagerDTO;
import com.fixzone.fixzon_backend.DTO.OwnerDTO;
import com.fixzone.fixzon_backend.service.ManagerService;
import com.fixzone.fixzon_backend.service.OwnerService;
import com.fixzone.fixzon_backend.service.ServiceCenterService;
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
    private final ServiceCenterService serviceCenterService;

    public ManagerController(ManagerService managerService, OwnerService ownerService, ServiceCenterService serviceCenterService) {
        this.managerService = managerService;
        this.ownerService = ownerService;
        this.serviceCenterService = serviceCenterService;
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
    public ResponseEntity<?> getCurrentOwnerManagers() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = getCurrentUserEmail();
        log.info("Fetching context for user: {}", email);

        boolean isManager = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SERVICE_MANAGER"));

        if (isManager) {
            // If the user is a manager, return their own profile
            ManagerDTO manager = managerService.getManagerByEmail(email);
            if (manager == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            return ResponseEntity.ok(manager);
        }

        // Otherwise, it's an owner trying to get all their managers
        OwnerDTO owner = ownerService.retrieveOwnerByEmail(email);
        if (owner == null) {
            log.warn("Owner not found for email: {}", email);
            return ResponseEntity.ok(List.of());
        }

        return ResponseEntity.ok(managerService.getManagersByOwnerCode(owner.getOwnerCode()));
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
    public ResponseEntity<?> createManager(@Valid @RequestBody ManagerDTO managerDTO) {
        try {
            String email = getCurrentUserEmail();
            OwnerDTO owner = ownerService.retrieveOwnerByEmail(email);
            
            if (owner != null && managerDTO.getManagedCenterId() != null) {
                boolean ownsCenter = serviceCenterService.getServiceCentersByOwnerCode(owner.getOwnerCode())
                    .stream().anyMatch(c -> c.getCenterId().equals(managerDTO.getManagedCenterId()));
                if (!ownsCenter) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You do not own this service center");
                }
            }
            
            log.info("Creating new manager: {}", managerDTO.getEmail());
            ManagerDTO newManager = managerService.createManager(managerDTO);

            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(newManager.getUserId())
                    .toUri();

            return ResponseEntity.created(location).body(newManager);
        } catch (IllegalArgumentException e) {
            log.warn("Manager creation rejected: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateManager(@PathVariable UUID id, @RequestBody ManagerDTO managerDTO) {
        try {
            String email = getCurrentUserEmail();
            OwnerDTO owner = ownerService.retrieveOwnerByEmail(email);
            
            ManagerDTO existing = managerService.getManagerById(id);
            if (existing == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (owner != null) {
                boolean ownsCenter = serviceCenterService.getServiceCentersByOwnerCode(owner.getOwnerCode())
                    .stream().anyMatch(c -> c.getCenterId().equals(existing.getManagedCenterId()));
                if (!ownsCenter) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You do not own this manager");
                }
                if (managerDTO.getManagedCenterId() != null && !managerDTO.getManagedCenterId().equals(existing.getManagedCenterId())) {
                    boolean ownsNewCenter = serviceCenterService.getServiceCentersByOwnerCode(owner.getOwnerCode())
                        .stream().anyMatch(c -> c.getCenterId().equals(managerDTO.getManagedCenterId()));
                    if (!ownsNewCenter) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You do not own the target service center");
                    }
                }
            }
            
            log.info("Updating manager ID: {}", id);
            ManagerDTO updatedManager = managerService.updateManager(id, managerDTO);
            return ResponseEntity.ok(updatedManager);
        } catch (IllegalArgumentException e) {
            log.warn("Manager update rejected: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/resend-invite")
    public ResponseEntity<?> resendInvite(@PathVariable UUID id) {
        try {
            String email = getCurrentUserEmail();
            OwnerDTO owner = ownerService.retrieveOwnerByEmail(email);
            ManagerDTO existing = managerService.getManagerById(id);
            if (existing == null) {
                return ResponseEntity.notFound().build();
            }
            if (owner != null) {
                boolean ownsCenter = serviceCenterService.getServiceCentersByOwnerCode(owner.getOwnerCode())
                    .stream().anyMatch(c -> c.getCenterId().equals(existing.getManagedCenterId()));
                if (!ownsCenter) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You do not own this manager");
                }
            }
            managerService.resendInvitation(id);
            return ResponseEntity.ok(java.util.Map.of("message", "Invitation resent successfully"));
        } catch (Exception e) {
            log.error("Failed to resend invite: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteManager(@PathVariable UUID id) {
        try {
            String email = getCurrentUserEmail();
            OwnerDTO owner = ownerService.retrieveOwnerByEmail(email);
            
            ManagerDTO existing = managerService.getManagerById(id);
            if (existing == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (owner != null) {
                boolean ownsCenter = serviceCenterService.getServiceCentersByOwnerCode(owner.getOwnerCode())
                    .stream().anyMatch(c -> c.getCenterId().equals(existing.getManagedCenterId()));
                if (!ownsCenter) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You do not own this manager");
                }
            }
            
            log.info("Deleting manager ID: {}", id);
            managerService.deleteManager(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            log.warn("Manager not found for deletion with ID: {}", id);
            return ResponseEntity.notFound().build();
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

