package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.ManagerDTO;
import com.fixzone.fixzon_backend.config.AppConstants;
import com.fixzone.fixzon_backend.model.Manager;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.repository.ManagerRepository;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SERVICE LAYER: ManagerService
 * This class orchestrates the lifecycle of Service Center Managers.
 * It enforces business rules, handles security (password hashing), 
 * and triggers notification workflows (welcome emails).
 */
@Service
public class ManagerService {
    private static final Logger log = LoggerFactory.getLogger(ManagerService.class);

    private final ManagerRepository managerRepository;
    private final ServiceCenterRepository serviceCenterRepository;
    private final OwnerRepository ownerRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final com.fixzone.fixzon_backend.repository.AuthRepository authRepository;
    
    @Value("${app.manager.default-password}")
    private String defaultPassword;

    /**
     * Constructor Injection: Ensures immutability and easier unit testing with mock objects.
     */
    public ManagerService(
            ManagerRepository managerRepository, 
            ServiceCenterRepository serviceCenterRepository,
            OwnerRepository ownerRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            com.fixzone.fixzon_backend.repository.AuthRepository authRepository) {
        this.managerRepository = managerRepository;
        this.serviceCenterRepository = serviceCenterRepository;
        this.ownerRepository = ownerRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.authRepository = authRepository;
    }

    /**
     * RETRIEVAL LOGIC: Scopes managers to a specific company owner.
     * Performs a multi-step lookup: Owner -> Service Centers -> Managers.
     */
    public List<ManagerDTO> getManagersByOwnerCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner code cannot be null or empty");
        }
        try {
            return ownerRepository.findByOwnerCode(code)
                    .map(owner -> {
                        List<UUID> centerIds = serviceCenterRepository.findByOwner_UserId(owner.getUserId())
                                .stream()
                                .map(ServiceCenter::getCenterId)
                                .collect(Collectors.toList());
                        
                        if (centerIds.isEmpty()) {
                            return List.<ManagerDTO>of();
                        }
                        
                        return managerRepository.findByManagedCenterIdIn(centerIds).stream()
                                .map(this::mapEntityToDto)
                                .collect(Collectors.toList());
                    })
                    .orElse(List.of());
        } catch (Exception e) {
            log.error("Database error while retrieving managers by owner code: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve managers by owner code", e);
        }
    }

    public List<ManagerDTO> getAllManagers() {
        try {
            return managerRepository.findAll().stream()
                    .map(this::mapEntityToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Database error while retrieving all managers: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve all managers", e);
        }
    }

    public ManagerDTO getManagerById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Manager ID cannot be null");
        }
        try {
            return managerRepository.findById(id)
                    .map(this::mapEntityToDto)
                    .orElse(null);
        } catch (Exception e) {
            log.error("Database error while retrieving manager by ID: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve manager by ID", e);
        }
    }

    public ManagerDTO getManagerByEmail(String email) {
        Objects.requireNonNull(email, "Email cannot be null");
        return managerRepository.findByEmailIgnoreCase(email.trim())
                .or(() -> managerRepository.findByEmail(email.trim()))
                .map(this::mapEntityToDto)
                .orElse(null);
    }

    /**
     * CREATION WORKFLOW: Initializes a new manager account.
     * Enforces default values, generates a unique secure password, hashes it, and optionally sends an invitation.
     */
    public ManagerDTO createManager(ManagerDTO managerDTO) {
        if (managerDTO == null) {
            throw new IllegalArgumentException("Manager data cannot be null");
        }
        if (managerDTO.getEmail() == null || managerDTO.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Manager email is required");
        }
        
        String cleanEmail = managerDTO.getEmail().trim().toLowerCase();
        if (!com.fixzone.fixzon_backend.util.EmailValidator.isValidRealEmail(cleanEmail)) {
            throw new IllegalArgumentException("Invalid email: The email domain does not have an active mail server (MX record) or is not recognized.");
        }

        if (authRepository.findByEmailIgnoreCase(cleanEmail).isPresent()) {
            throw new IllegalArgumentException("An account with this email address already exists in the system");
        }

        try {
            Manager manager = mapDtoToEntity(managerDTO);
            manager.setEmail(cleanEmail);
            
            // INITIALIZATION: Setup critical fields if they are missing
            if (manager.getUserId() == null) {
                manager.setUserId(UUID.randomUUID());
            }
            
            manager.setRole(AppConstants.ROLE_SERVICE_MANAGER);
            
            boolean shouldSendInvite = managerDTO.getSendInvite() == null || managerDTO.getSendInvite();
            manager.setStatus(shouldSendInvite ? "INVITED" : (managerDTO.getStatus() != null ? managerDTO.getStatus() : AppConstants.STATUS_ACTIVE));
            manager.setEmailVerified(!shouldSendInvite);
            
            // UNIQUE IDENTIFIER: Create a human-readable manager code for internal tracking
            if (manager.getManagerCode() == null || manager.getManagerCode().isEmpty()) {
                manager.setManagerCode(AppConstants.MANAGER_PREFIX + manager.getUserId().toString().substring(0, 8).toUpperCase());
            }

            // Generate unique secure temporary password for each manager
            String rawPassword = (managerDTO.getPasswordHash() != null && !managerDTO.getPasswordHash().trim().isEmpty()) 
                    ? managerDTO.getPasswordHash().trim() 
                    : com.fixzone.fixzon_backend.util.PasswordGenerator.generateUniquePassword(10);
            manager.setPasswordHash(passwordEncoder.encode(rawPassword));
            
            Manager savedManager = managerRepository.save(manager);

            // Fetch center & company name for rich email template
            String centerName = null;
            String companyName = null;
            if (savedManager.getManagedCenterId() != null) {
                var centerOpt = serviceCenterRepository.findById(savedManager.getManagedCenterId());
                if (centerOpt.isPresent()) {
                    centerName = centerOpt.get().getName();
                    if (centerOpt.get().getOwner() != null) {
                        var ownerOpt = ownerRepository.findById(centerOpt.get().getOwner().getUserId());
                        if (ownerOpt.isPresent()) {
                            companyName = ownerOpt.get().getCompanyName();
                        }
                    }
                }
            }

            // NOTIFICATION: Trigger credentials email with the unique password
            if (shouldSendInvite) {
                emailService.sendManagerCredentialsEmail(savedManager.getEmail(), savedManager.getFullName(), rawPassword, centerName, companyName);
            }

            ManagerDTO response = mapEntityToDto(savedManager);
            response.setSendInvite(shouldSendInvite);
            return response;
        } catch (Exception e) {
            log.error("Database error while creating manager: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create manager", e);
        }
    }

    /**
     * Resends manager login credentials email with a newly generated unique password.
     */
    public void resendInvitation(UUID managerId) {
        if (managerId == null) {
            throw new IllegalArgumentException("Manager ID cannot be null");
        }
        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("Manager not found"));
        
        // Generate a new unique password on resend
        String newPassword = com.fixzone.fixzon_backend.util.PasswordGenerator.generateUniquePassword(10);
        manager.setPasswordHash(passwordEncoder.encode(newPassword));
        manager.setStatus("INVITED");
        manager.setEmailVerified(false);
        manager.setUpdatedAt(LocalDateTime.now());
        managerRepository.save(manager);

        String centerName = null;
        String companyName = null;
        if (manager.getManagedCenterId() != null) {
            var centerOpt = serviceCenterRepository.findById(manager.getManagedCenterId());
            if (centerOpt.isPresent()) {
                centerName = centerOpt.get().getName();
                if (centerOpt.get().getOwner() != null) {
                    var ownerOpt = ownerRepository.findById(centerOpt.get().getOwner().getUserId());
                    if (ownerOpt.isPresent()) {
                        companyName = ownerOpt.get().getCompanyName();
                    }
                }
            }
        }

        emailService.sendManagerCredentialsEmail(manager.getEmail(), manager.getFullName(), newPassword, centerName, companyName);
    }

    /**
     * UPDATE LOGIC: Performs a partial update to preserve existing data.
     * Explicitly checks each field to avoid overriding data with null values.
     */
    public ManagerDTO updateManager(UUID id, ManagerDTO dto) {
        if (id == null) {
            throw new IllegalArgumentException("Target ID for update cannot be null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Manager data cannot be null");
        }
        
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            String cleanEmail = dto.getEmail().trim().toLowerCase();
            if (!com.fixzone.fixzon_backend.util.EmailValidator.isValidRealEmail(cleanEmail)) {
                throw new IllegalArgumentException("Invalid email: The email domain does not have an active mail server (MX record) or is not recognized.");
            }
            java.util.Optional<com.fixzone.fixzon_backend.model.User> existingUser = authRepository.findByEmail(cleanEmail);
            if (existingUser.isPresent() && !existingUser.get().getUserId().equals(id)) {
                throw new IllegalArgumentException("An account with this email address already exists in the system");
            }
        }
        
        try {
            return managerRepository.findById(id).map(existing -> {
                if (dto.getFullName() != null) existing.setFullName(dto.getFullName());
                if (dto.getEmail() != null) existing.setEmail(dto.getEmail().trim().toLowerCase());
                if (dto.getPhone() != null) existing.setPhone(dto.getPhone());
                if (dto.getManagedCenterId() != null) existing.setManagedCenterId(dto.getManagedCenterId());
                if (dto.getStatus() != null) existing.setStatus(dto.getStatus());
                if (dto.getProfilePictureUrl() != null) existing.setProfilePictureUrl(dto.getProfilePictureUrl());
                if (dto.getEmailVerified() != null) existing.setEmailVerified(dto.getEmailVerified());
                
                if (dto.getPasswordHash() != null && !dto.getPasswordHash().isEmpty()) {
                    existing.setPasswordHash(passwordEncoder.encode(dto.getPasswordHash()));
                }

                existing.setUpdatedAt(LocalDateTime.now());
                return mapEntityToDto(managerRepository.save(existing));
            }).orElse(null);
        } catch (Exception e) {
            log.error("Database error while updating manager: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update manager", e);
        }
    }

    public void resendInvite(UUID managerId) {
        if (managerId == null) {
            throw new IllegalArgumentException("Manager ID cannot be null");
        }
        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new IllegalStateException("Manager not found with id: " + managerId));
        
        emailService.sendWelcomeEmail(manager.getEmail(), manager.getFullName(), defaultPassword);
    }

    public void deleteManager(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID for deletion cannot be null");
        }
        try {
            if (managerRepository.existsById(id)) {
                managerRepository.deleteById(id);
            }
        } catch (Exception e) {
            log.error("Database error while deleting manager: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete manager", e);
        }
    }

    /**
     * MAPPING HELPERS: Standardizes the conversion between API and DB models.
     * This keeps the controller and repository layers clean.
     */
    private ManagerDTO mapEntityToDto(Manager entity) {
        if (entity == null) return null;
        ManagerDTO dto = new ManagerDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    private Manager mapDtoToEntity(ManagerDTO dto) {
        if (dto == null) return null;
        Manager entity = new Manager();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}
