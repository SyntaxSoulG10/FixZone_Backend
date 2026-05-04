package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.ManagerDTO;
import com.fixzone.fixzon_backend.config.AppConstants;
import com.fixzone.fixzon_backend.model.Manager;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.repository.ManagerRepository;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.UserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SERVICE LAYER: ManagerService
 * Orchestrates the complete lifecycle of Service Center Managers
 * RESPONSIBILITIES:
 * - Manager account creation with email verification
 * - Password security (hashing via BCrypt)
 * - Welcome email notifications
 * - Multi-step retrieval with owner scoping
 * - Profile management and status tracking
 * - Enforces business rules (unique emails, valid center assignments)
 */
@Service
public class ManagerService {

    // Repository for manager database operations
    private final ManagerRepository managerRepository;
    
    // Repository for service center validation
    private final ServiceCenterRepository serviceCenterRepository;
    
    // Repository for owner lookups
    private final OwnerRepository ownerRepository;
    
    // Repository for user validation (email uniqueness checks)
    private final UserRepository userRepository;
    
    // Password encoder for secure credential storage
    private final PasswordEncoder passwordEncoder;
    
    // Email service for welcome invitations
    private final EmailService emailService;
    
    // Injected default password from application configuration
    @Value("${app.manager.default-password}")
    private String defaultPassword;

    /**
     * Constructor-based dependency injection
     * Ensures immutability and simplifies unit testing with mock objects
     */
    public ManagerService(
            ManagerRepository managerRepository, 
            ServiceCenterRepository serviceCenterRepository,
            OwnerRepository ownerRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.managerRepository = managerRepository;
        this.serviceCenterRepository = serviceCenterRepository;
        this.ownerRepository = ownerRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Retrieves all managers assigned to a specific company owner
     * RETRIEVAL FLOW: Owner code → Owner entity → Service centers → Managers
     * SCOPED ACCESS: Only managers managing centers owned by the specified owner are returned
     * @param code Owner code (unique identifier)
     * @return List of ManagerDTOs for the owner's service centers, empty if none found
     */
    public List<ManagerDTO> getManagersByOwnerCode(String code) {
        // Validate input parameter
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner code cannot be null or empty");
        }
        try {
            // Step 1: Look up owner by code
            return ownerRepository.findByOwnerCode(code)
                    .map(owner -> {
                        // Step 2: Find all service centers owned by this owner
                        List<UUID> centerIds = serviceCenterRepository.findByOwner_UserId(owner.getUserId())
                                .stream()
                                .map(ServiceCenter::getCenterId)
                                .collect(Collectors.toList());
                        
                        // Step 3: Return empty list if owner has no service centers
                        if (centerIds.isEmpty()) {
                            return List.<ManagerDTO>of();
                        }
                        
                        // Step 4: Find all managers managing these centers and convert to DTOs
                        return managerRepository.findByManagedCenterIdIn(centerIds).stream()
                                .map(this::mapEntityToDto)
                                .collect(Collectors.toList());
                    })
                    .orElse(List.of());
        } catch (Exception e) {
            System.err.println("Database error while retrieving managers by owner code: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve managers by owner code", e);
        }
    }

    /**
     * Retrieves all managers across the entire system
     * @return List of all ManagerDTOs
     */
    public List<ManagerDTO> getAllManagers() {
        try {
            // Fetch all managers and convert to DTOs
            return managerRepository.findAll().stream()
                    .map(this::mapEntityToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Database error while retrieving all managers: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve all managers", e);
        }
    }

    /**
     * Retrieves a specific manager by UUID
     * @param id UUID of the manager
     * @return ManagerDTO if found, null otherwise
     * @throws IllegalArgumentException if id is null
     */
    public ManagerDTO getManagerById(UUID id) {
        // Validate input parameter
        if (id == null) {
            throw new IllegalArgumentException("Manager ID cannot be null");
        }
        try {
            // Query by ID and convert to DTO
            return managerRepository.findById(id)
                    .map(this::mapEntityToDto)
                    .orElse(null);
        } catch (Exception e) {
            System.err.println("Database error while retrieving manager by ID: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve manager by ID", e);
        }
    }

    /**
     * Creates a new manager account with onboarding workflow
     * ONBOARDING STEPS:
     * 1. Validate email uniqueness across platform
     * 2. Verify assigned service center exists
     * 3. Generate unique manager code
     * 4. Hash password securely
     * 5. Send welcome email with credentials
     * @param managerDTO ManagerDTO with account details
     * @return Created ManagerDTO with generated ID and code
     * @throws IllegalArgumentException if required fields missing
     * @throws RuntimeException if validation fails
     */
    public ManagerDTO createManager(ManagerDTO managerDTO) {
        // Step 1: Validate DTO is not null
        if (managerDTO == null) {
            throw new IllegalArgumentException("Manager data cannot be null");
        }
        // Step 2: Validate email is provided (required field)
        if (managerDTO.getEmail() == null || managerDTO.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Manager email is required");
        }
        
        // Step 3: UNIQUE EMAIL CHECK - Prevent duplicate accounts across entire platform
        if (userRepository.existsByEmail(managerDTO.getEmail())) {
            throw new RuntimeException("Email '" + managerDTO.getEmail() + "' is already in use by another account");
        }

        // Step 4: CENTER EXISTENCE CHECK - Ensure assigned service center is valid
        if (managerDTO.getManagedCenterId() == null || !serviceCenterRepository.existsById(managerDTO.getManagedCenterId())) {
            throw new RuntimeException("A valid service center must be assigned to the manager");
        }
        
        try {
            // Step 5: Convert DTO to entity
            Manager manager = mapDtoToEntity(managerDTO);
            
            // Step 6: INITIALIZATION - Setup critical fields if missing
            if (manager.getUserId() == null) {
                manager.setUserId(UUID.randomUUID());
            }
            
            // Set default role and status
            manager.setRole(AppConstants.ROLE_SERVICE_MANAGER);
            manager.setStatus(manager.getStatus() != null ? manager.getStatus() : AppConstants.STATUS_ACTIVE);
            manager.setCreatedAt(LocalDateTime.now());
            
            // Step 7: UNIQUE IDENTIFIER - Create human-readable manager code for internal tracking
            if (manager.getManagerCode() == null || manager.getManagerCode().isEmpty()) {
                manager.setManagerCode(AppConstants.MANAGER_PREFIX + manager.getUserId().toString().substring(0, 8).toUpperCase());
            }

            // Step 8: SECURITY - Hash password using BCrypt (never store raw passwords)
            String rawPassword = (managerDTO.getPasswordHash() != null && !managerDTO.getPasswordHash().isEmpty()) 
                    ? managerDTO.getPasswordHash() : defaultPassword;
            
            if (rawPassword == null || rawPassword.isEmpty()) {
                throw new RuntimeException("No default password configured and none provided");
            }
            
            manager.setPasswordHash(passwordEncoder.encode(rawPassword));
            
            // Step 9: Persist manager to database
            Manager savedManager = managerRepository.save(manager);

            // Step 10: NOTIFICATION - Trigger welcome email for onboarding
            boolean shouldSendInvite = managerDTO.getSendInvite() == null || managerDTO.getSendInvite();
            if (shouldSendInvite) {
                emailService.sendWelcomeEmail(savedManager.getEmail(), savedManager.getFullName(), rawPassword);
            }

            // Step 11: Return DTO with send invitation flag
            ManagerDTO response = mapEntityToDto(savedManager);
            response.setSendInvite(shouldSendInvite);
            return response;
        } catch (RuntimeException e) {
            throw e; // Re-throw business logic exceptions
        } catch (Exception e) {
            System.err.println("Critical error while creating manager: " + e.getMessage());
            throw new RuntimeException("Failed to onboard manager due to a system error. Please contact support.", e);
        }
    }

    /**
     * Updates an existing manager's profile
     * PARTIAL UPDATE: Only non-null fields are updated to preserve existing data
     * PASSWORD HANDLING: Passwords are re-hashed if provided
     * @param id UUID of manager to update
     * @param dto ManagerDTO with updated values
     * @return Updated ManagerDTO
     * @throws IllegalArgumentException if id or dto is null
     */
    public ManagerDTO updateManager(UUID id, ManagerDTO dto) {
        // Validate input parameters
        if (id == null) {
            throw new IllegalArgumentException("Target ID for update cannot be null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Manager data cannot be null");
        }
        
        try {
            // Step 1: Fetch existing manager
            return managerRepository.findById(id).map(existing -> {
                // Step 2: Update only non-null fields to preserve existing data
                if (dto.getFullName() != null) existing.setFullName(dto.getFullName());
                if (dto.getEmail() != null) existing.setEmail(dto.getEmail());
                if (dto.getPhone() != null) existing.setPhone(dto.getPhone());
                if (dto.getManagedCenterId() != null) existing.setManagedCenterId(dto.getManagedCenterId());
                if (dto.getStatus() != null) existing.setStatus(dto.getStatus());
                if (dto.getProfilePictureUrl() != null) existing.setProfilePictureUrl(dto.getProfilePictureUrl());
                if (dto.getEmailVerified() != null) existing.setEmailVerified(dto.getEmailVerified());
                
                // Step 3: If password is provided, hash it using BCrypt
                if (dto.getPasswordHash() != null && !dto.getPasswordHash().isEmpty()) {
                    existing.setPasswordHash(passwordEncoder.encode(dto.getPasswordHash()));
                }

                // Step 4: Record update timestamp
                existing.setUpdatedAt(LocalDateTime.now());
                // Step 5: Persist and return updated manager as DTO
                return mapEntityToDto(managerRepository.save(existing));
            }).orElse(null);
        } catch (Exception e) {
            System.err.println("Database error while updating manager: " + e.getMessage());
            throw new RuntimeException("Failed to update manager", e);
        }
    }

    /**
     * Deletes a manager account from the system
     * @param id UUID of manager to delete
     * @throws IllegalArgumentException if id is null
     * @throws IllegalStateException if manager not found
     */
    public void deleteManager(UUID id) {
        // Validate input parameter
        if (id == null) {
            throw new IllegalArgumentException("ID for deletion cannot be null");
        }
        try {
            // Step 1: Verify manager exists before deletion
            if (!managerRepository.existsById(id)) {
                throw new IllegalStateException("Manager not found with id: " + id);
            }
            // Step 2: Delete manager from database
            managerRepository.deleteById(id);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Database error while deleting manager: " + e.getMessage());
            throw new RuntimeException("Failed to delete manager", e);
        }
    }

    /**
     * Converts Manager entity to ManagerDTO
     * Maintains clean separation between database and API layers
     * @param entity Manager entity from database
     * @return ManagerDTO with all properties mapped, null if input is null
     */
    private ManagerDTO mapEntityToDto(Manager entity) {
        // Handle null input safely
        if (entity == null) return null;
        // Create DTO and copy all properties using Spring BeanUtils
        ManagerDTO dto = new ManagerDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    /**
     * Converts ManagerDTO to Manager entity
     * Prepares DTO data for database persistence
     * @param dto ManagerDTO with data to persist
     * @return Manager entity, null if input is null
     */
    private Manager mapDtoToEntity(ManagerDTO dto) {
        // Handle null input safely
        if (dto == null) return null;
        // Create entity and copy all properties from DTO
        Manager entity = new Manager();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}
