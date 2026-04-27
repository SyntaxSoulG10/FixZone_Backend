package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.OwnerDTO;
import com.fixzone.fixzon_backend.model.Owner;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
import org.springframework.beans.BeanUtils;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for managing Company Owner profiles.
 * It handles the lifecycle of owner data, including registration, updates, and retrieval.
 */
@Service
public class OwnerService {

    private final OwnerRepository ownerRepository;
    private final ImageKitService imageKitService;

    public OwnerService(OwnerRepository ownerRepository, ImageKitService imageKitService) {
        this.ownerRepository = ownerRepository;
        this.imageKitService = imageKitService;
    }

    /**
     * Retrieves all owners registered in the system.
     * We map entities to DTOs to maintain a clean separation between DB and API layers.
     */
    public List<OwnerDTO> retrieveAllOwners() {
        return ownerRepository.findAll().stream()
                .map(this::transformToDataTransferObject)
                .collect(Collectors.toList());
    }

    public OwnerDTO retrieveOwnerById(UUID targetOwnerId) {
        Objects.requireNonNull(targetOwnerId, "The Owner ID parameter must not be null.");
        return ownerRepository.findById(targetOwnerId)
                .map(this::transformToDataTransferObject)
                .orElse(null);
    }

    public OwnerDTO retrieveOwnerByCode(String ownerCode) {
        return ownerRepository.findByOwnerCode(ownerCode)
                .map(this::transformToDataTransferObject)
                .orElse(null);
    }

    public OwnerDTO retrieveOwnerByEmail(String email) {
        return ownerRepository.findByEmail(email)
                .map(this::transformToDataTransferObject)
                .orElse(null);
    }

    /**
     * Registers a new owner. We generate a unique UUID if one isn't provided.
     */
    public OwnerDTO registerOwner(OwnerDTO newOwnerRegistrationData) {
        Objects.requireNonNull(newOwnerRegistrationData, "Registration data must not be null.");
        // Transform the DTO back to a JPA Entity because repositories only understand Entities.
        Owner newOwnerEntity = Objects.requireNonNull(transformToDatabaseEntity(newOwnerRegistrationData));
        
        // Ensure a unique identifier exists before saving to the database.
        if (newOwnerEntity.getUserId() == null) {
            newOwnerEntity.setUserId(UUID.randomUUID());
        }
        
        Owner persistedOwnerEntity = Objects.requireNonNull(ownerRepository.save(newOwnerEntity));
        return transformToDataTransferObject(persistedOwnerEntity);
    }

    /**
     * Updates an existing owner's details.
     * We explicitly map fields to ensure inherited user properties (like profile picture) are preserved.
     */
    public OwnerDTO modifyOwner(UUID targetOwnerId, OwnerDTO updatedOwnerData) {
        Objects.requireNonNull(targetOwnerId, "The Owner ID parameter must not be null.");
        
        try {
            return ownerRepository.findById(targetOwnerId).map(existingOwner -> {
                // Update Owner-specific business fields
                if (updatedOwnerData.getOwnerCode() != null) existingOwner.setOwnerCode(updatedOwnerData.getOwnerCode());
                if (updatedOwnerData.getCompanyName() != null) existingOwner.setCompanyName(updatedOwnerData.getCompanyName());
                if (updatedOwnerData.getCompanyEmail() != null) existingOwner.setCompanyEmail(updatedOwnerData.getCompanyEmail());
                if (updatedOwnerData.getCompanyNumber() != null) existingOwner.setCompanyNumber(updatedOwnerData.getCompanyNumber());
                if (updatedOwnerData.getBannerImageUrl() != null && !updatedOwnerData.getBannerImageUrl().equals(existingOwner.getBannerImageUrl())) {
                    System.out.println("[OWNER] Detected change in Banner Image. Length: " + updatedOwnerData.getBannerImageUrl().length());
                    String uploadedUrl = imageKitService.uploadImage(updatedOwnerData.getBannerImageUrl(), "owner-banner-" + existingOwner.getUserId());
                    existingOwner.setBannerImageUrl(uploadedUrl);
                    System.out.println("[OWNER] Banner updated to: " + uploadedUrl);
                } else {
                    System.out.println("[OWNER] No change in Banner Image detected.");
                }
                
                // Update inherited User profile fields
                if (updatedOwnerData.getFullName() != null) existingOwner.setFullName(updatedOwnerData.getFullName());
                if (updatedOwnerData.getEmail() != null) existingOwner.setEmail(updatedOwnerData.getEmail());
                if (updatedOwnerData.getPhone() != null) existingOwner.setPhone(updatedOwnerData.getPhone());
                
                if (updatedOwnerData.getProfilePictureUrl() != null && !updatedOwnerData.getProfilePictureUrl().equals(existingOwner.getProfilePictureUrl())) {
                    System.out.println("[OWNER] Detected change in Profile Picture. Length: " + updatedOwnerData.getProfilePictureUrl().length());
                    String uploadedUrl = imageKitService.uploadImage(updatedOwnerData.getProfilePictureUrl(), "owner-profile-" + existingOwner.getUserId());
                    existingOwner.setProfilePictureUrl(uploadedUrl);
                    System.out.println("[OWNER] Profile picture updated to: " + uploadedUrl);
                } else {
                    System.out.println("[OWNER] No change in Profile Picture detected.");
                }
                
                if (updatedOwnerData.getStatus() != null) existingOwner.setStatus(updatedOwnerData.getStatus());
                
                Owner successfullyUpdatedEntity = ownerRepository.save(existingOwner);
                return transformToDataTransferObject(successfullyUpdatedEntity);
            }).orElse(null);
        } catch (Exception e) {
            // Logging errors is critical for debugging 500 status codes in production.
            System.err.println("CRITICAL ERROR during owner modification: " + e.getMessage());
            throw e;
        }
    }

    public void removeOwner(UUID targetOwnerId) {
        Objects.requireNonNull(targetOwnerId, "The Owner ID parameter must not be null.");
        ownerRepository.deleteById(targetOwnerId);
    }

    /**
     * Transforms a Database Entity to a Data Transfer Object (DTO).
     * This protects internal database structures from being exposed directly to the frontend.
     */
    private OwnerDTO transformToDataTransferObject(Owner sourceOwnerEntity) {
        if (sourceOwnerEntity == null) return null;
        
        OwnerDTO resultantDto = new OwnerDTO();
        BeanUtils.copyProperties(sourceOwnerEntity, resultantDto);
        
        // Explicitly map inherited User fields to ensure consistency in the frontend
        resultantDto.setUserId(sourceOwnerEntity.getUserId());
        resultantDto.setFullName(sourceOwnerEntity.getFullName());
        resultantDto.setEmail(sourceOwnerEntity.getEmail());
        resultantDto.setPhone(sourceOwnerEntity.getPhone());
        resultantDto.setRole(sourceOwnerEntity.getRole());
        resultantDto.setProfilePictureUrl(sourceOwnerEntity.getProfilePictureUrl());
        resultantDto.setStatus(sourceOwnerEntity.getStatus());
        resultantDto.setCreatedAt(sourceOwnerEntity.getCreatedAt());
        resultantDto.setUpdatedAt(sourceOwnerEntity.getUpdatedAt());
        
        return resultantDto;
    }

    private Owner transformToDatabaseEntity(OwnerDTO sourceOwnerData) {
        if (sourceOwnerData == null) return null;
        Owner resultantEntity = new Owner();
        BeanUtils.copyProperties(sourceOwnerData, resultantEntity);
        return resultantEntity;
    }
}
