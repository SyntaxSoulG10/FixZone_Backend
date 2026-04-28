package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.ManagerDTO;
import com.fixzone.fixzon_backend.config.AppConstants;
import com.fixzone.fixzon_backend.model.Manager;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.repository.ManagerRepository;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import org.springframework.beans.BeanUtils;
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

    private final ManagerRepository managerRepository;
    private final ServiceCenterRepository serviceCenterRepository;
    private final OwnerRepository ownerRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * Constructor Injection: Ensures immutability and easier unit testing with mock objects.
     */
    public ManagerService(
            ManagerRepository managerRepository, 
            ServiceCenterRepository serviceCenterRepository,
            OwnerRepository ownerRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.managerRepository = managerRepository;
        this.serviceCenterRepository = serviceCenterRepository;
        this.ownerRepository = ownerRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * RETRIEVAL LOGIC: Scopes managers to a specific company owner.
     * Performs a multi-step lookup: Owner -> Service Centers -> Managers.
     */
    public List<ManagerDTO> getManagersByOwnerCode(String code) {
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
    }

    public List<ManagerDTO> getAllManagers() {
        return managerRepository.findAll().stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    public ManagerDTO getManagerById(UUID id) {
        Objects.requireNonNull(id, "Manager ID cannot be null");
        return managerRepository.findById(id)
                .map(this::mapEntityToDto)
                .orElse(null);
    }

    /**
     * CREATION WORKFLOW: Initializes a new manager account.
     * Enforces default values, hashes passwords, and optionally sends an invitation.
     */
    public ManagerDTO createManager(ManagerDTO managerDTO) {
        Manager manager = mapDtoToEntity(managerDTO);
        
        // INITIALIZATION: Setup critical fields if they are missing
        if (manager.getUserId() == null) {
            manager.setUserId(UUID.randomUUID());
        }
        
        manager.setRole(AppConstants.ROLE_SERVICE_MANAGER);
        manager.setStatus(manager.getStatus() != null ? manager.getStatus() : AppConstants.STATUS_ACTIVE);
        
        // UNIQUE IDENTIFIER: Create a human-readable manager code for internal tracking
        if (manager.getManagerCode() == null || manager.getManagerCode().isEmpty()) {
            manager.setManagerCode(AppConstants.MANAGER_PREFIX + manager.getUserId().toString().substring(0, 8).toUpperCase());
        }

        // SECURITY: Never store raw passwords. Hashing prevents leaks if the DB is compromised.
        String rawPassword = (managerDTO.getPasswordHash() != null && !managerDTO.getPasswordHash().isEmpty()) 
                ? managerDTO.getPasswordHash() : AppConstants.DEFAULT_PASSWORD;
        manager.setPasswordHash(passwordEncoder.encode(rawPassword));
        
        Manager savedManager = managerRepository.save(manager);

        // NOTIFICATION: Trigger welcome email to help user onboard immediately
        boolean shouldSendInvite = managerDTO.getSendInvite() == null || managerDTO.getSendInvite();
        if (shouldSendInvite) {
            emailService.sendWelcomeEmail(savedManager.getEmail(), savedManager.getFullName(), rawPassword);
        }

        ManagerDTO response = mapEntityToDto(savedManager);
        response.setSendInvite(shouldSendInvite);
        return response;
    }

    /**
     * UPDATE LOGIC: Performs a partial update to preserve existing data.
     * Explicitly checks each field to avoid overriding data with null values.
     */
    public ManagerDTO updateManager(UUID id, ManagerDTO dto) {
        Objects.requireNonNull(id, "Target ID for update cannot be null");
        
        return managerRepository.findById(id).map(existing -> {
            if (dto.getFullName() != null) existing.setFullName(dto.getFullName());
            if (dto.getEmail() != null) existing.setEmail(dto.getEmail());
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
    }

    public void deleteManager(UUID id) {
        Objects.requireNonNull(id, "ID for deletion cannot be null");
        managerRepository.deleteById(id);
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
