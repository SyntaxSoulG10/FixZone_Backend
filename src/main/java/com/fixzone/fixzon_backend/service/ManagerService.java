package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.ManagerDTO;
import com.fixzone.fixzon_backend.model.Manager;
import com.fixzone.fixzon_backend.repository.ManagerRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class ManagerService {

    // Dependency injection via constructor keeps strictly initialized references 
    // ensuring the repository cannot be null at runtime.
    private final ManagerRepository managerRepository;
    private final ServiceCenterRepository serviceCenterRepository;
    private final OwnerRepository ownerRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public ManagerService(ManagerRepository managerRepository, 
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

    public List<ManagerDTO> getManagersByOwnerCode(String code) {
        return ownerRepository.findByOwnerCode(code)
                .map(owner -> {
                    List<UUID> centerIds = serviceCenterRepository.findByOwner_UserId(owner.getUserId())
                            .stream()
                            .map(ServiceCenter::getCenterId)
                            .collect(Collectors.toList());
                    if (centerIds.isEmpty()) return List.<ManagerDTO>of();
                    return managerRepository.findByManagedCenterIdIn(centerIds).stream()
                            .map(this::transformToDataTransferObject)
                            .collect(Collectors.toList());
                })
                .orElse(List.of());
    }

    public List<ManagerDTO> getAllManagers() {
        // Enforce the separation between DB layers and API presentation layers
        // by always mapping outbound entities into pure DTOs.
        return managerRepository.findAll().stream()
                .map(this::transformToDataTransferObject)
                .collect(Collectors.toList());
    }

    public ManagerDTO getManagerById(UUID id) {
        Objects.requireNonNull(id, "ID must not be null");
        return managerRepository.findById(id)
                .map(this::transformToDataTransferObject)
                .orElse(null);
    }

    public ManagerDTO createManager(ManagerDTO managerDTO) {
        Manager manager = transformToDatabaseEntity(managerDTO);
        if (manager.getUserId() == null) {
            manager.setUserId(UUID.randomUUID());
        }
        if (manager.getRole() == null) {
            manager.setRole("Manager");
        }
        if (manager.getStatus() == null) {
            manager.setStatus("Active");
        }
        // Generate a manager code if not provided
        if (manager.getManagerCode() == null || manager.getManagerCode().isEmpty()) {
            manager.setManagerCode("MGR-" + manager.getUserId().toString().substring(0, 8).toUpperCase());
        }

        String rawPassword = managerDTO.getPasswordHash();
        if (rawPassword == null || rawPassword.isEmpty()) {
            rawPassword = "Manager123!"; // Default password
        }

        manager.setPasswordHash(passwordEncoder.encode(rawPassword));
        
        Manager savedManager = managerRepository.save(manager);

        // Send welcome email if requested (default to true if null)
        boolean shouldSend = managerDTO.getSendInvite() == null || Boolean.TRUE.equals(managerDTO.getSendInvite());
        
        if (shouldSend) {
            emailService.sendWelcomeEmail(savedManager.getEmail(), savedManager.getFullName(), rawPassword);
        }

        ManagerDTO responseDto = transformToDataTransferObject(savedManager);
        responseDto.setSendInvite(shouldSend);
        return responseDto;
    }

    public ManagerDTO updateManager(UUID id, ManagerDTO managerDTO) {
        Objects.requireNonNull(id, "ID must not be null");
        return managerRepository.findById(id).map(existingManager -> {
            // Only update fields that are provided in the DTO
            if (managerDTO.getFullName() != null) existingManager.setFullName(managerDTO.getFullName());
            if (managerDTO.getEmail() != null) existingManager.setEmail(managerDTO.getEmail());
            if (managerDTO.getPhone() != null) existingManager.setPhone(managerDTO.getPhone());
            if (managerDTO.getManagedCenterId() != null) existingManager.setManagedCenterId(managerDTO.getManagedCenterId());
            if (managerDTO.getStatus() != null) existingManager.setStatus(managerDTO.getStatus());
            if (managerDTO.getProfilePictureUrl() != null) existingManager.setProfilePictureUrl(managerDTO.getProfilePictureUrl());
            if (managerDTO.getEmailVerified() != null) existingManager.setEmailVerified(managerDTO.getEmailVerified());
            
            // Password update should be handled carefully
            if (managerDTO.getPasswordHash() != null && !managerDTO.getPasswordHash().isEmpty()) {
                existingManager.setPasswordHash(passwordEncoder.encode(managerDTO.getPasswordHash()));
            }

            existingManager.setUpdatedAt(java.time.LocalDateTime.now());
            Manager savedManager = managerRepository.save(existingManager);
            return transformToDataTransferObject(savedManager);
        }).orElse(null);
    }

    public void deleteManager(UUID id) {
        Objects.requireNonNull(id, "ID must not be null");
        managerRepository.deleteById(id);
    }

    // Extracted transformation logic ensures the business layer is decoupled purely 
    // from structural changes to the entity models.
    private ManagerDTO transformToDataTransferObject(Manager manager) {
        if (manager == null) return null;
        ManagerDTO dto = new ManagerDTO();
        BeanUtils.copyProperties(manager, dto);
        return dto;
    }

    private Manager transformToDatabaseEntity(ManagerDTO dto) {
        if (dto == null) return null;
        Manager manager = new Manager();
        BeanUtils.copyProperties(dto, manager);
        return manager;
    }
}
