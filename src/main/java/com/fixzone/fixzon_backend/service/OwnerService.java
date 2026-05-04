package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.OwnerDTO;
import com.fixzone.fixzon_backend.config.AppConstants;
import com.fixzone.fixzon_backend.model.Owner;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for managing Company Owner profiles.
 * Handles the lifecycle of owner data, including registration, updates, and retrieval.
 */
@Service
public class OwnerService {

    private final OwnerRepository ownerRepository;
    private final ImageKitService imageKitService;

    /**
     * Constructor injection for required dependencies.
     */
    public OwnerService(OwnerRepository ownerRepository, ImageKitService imageKitService) {
        this.ownerRepository = ownerRepository;
        this.imageKitService = imageKitService;
    }

    // Retrieves all owners and converts to DTOs
    public List<OwnerDTO> retrieveAllOwners() {
        try {
            return ownerRepository.findAll().stream()
                .map(this::transformToDataTransferObject)
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Database error while retrieving owners: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve all owners", e);
        }
    }

    // Retrieves owner by ID
    public OwnerDTO retrieveOwnerById(UUID targetOwnerId) {
        if (targetOwnerId == null) {
            throw new IllegalArgumentException("The Owner ID parameter must not be null.");
        }
        try {
            return ownerRepository.findById(targetOwnerId)
                .map(this::transformToDataTransferObject)
                .orElse(null);
        } catch (Exception e) {
            System.err.println("Database error while retrieving owner by ID: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve owner by ID", e);
        }
    }

    // Retrieves owner by owner code
    public OwnerDTO retrieveOwnerByCode(String ownerCode) {
        if (ownerCode == null || ownerCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner code must not be null or empty.");
        }
        try {
            return ownerRepository.findByOwnerCode(ownerCode)
                .map(this::transformToDataTransferObject)
                .orElse(null);
        } catch (Exception e) {
            System.err.println("Database error while retrieving owner by code: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve owner by code", e);
        }
    }

    /**
     * Retrieves a specific owner by email address
     * @param email Email address of the owner
     * @return Owner DTO if found, null otherwise
     * @throws IllegalArgumentException if email is null or empty
     */
    public OwnerDTO retrieveOwnerByEmail(String email) {
        // Validate input parameter
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email must not be null or empty.");
        }
        try {
            // Query database by email and convert to DTO
            return ownerRepository.findByEmail(email)
                .map(this::transformToDataTransferObject)
                .orElse(null);
        } catch (Exception e) {
            System.err.println("Database error while retrieving owner by email: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve owner by email", e);
        }
    }

    // Registers new owner (validates email uniqueness, generates UUID)
    public OwnerDTO registerOwner(OwnerDTO newOwnerRegistrationData) {
        if (newOwnerRegistrationData == null) {
            throw new IllegalArgumentException("Registration data must not be null.");
        }
        if (newOwnerRegistrationData.getEmail() == null || newOwnerRegistrationData.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required for registration.");
        }

        try {
            if (ownerRepository.findByEmail(newOwnerRegistrationData.getEmail()).isPresent()) {
                throw new IllegalStateException("An owner with this email already exists.");
            }

            Owner newOwnerEntity = transformToDatabaseEntity(newOwnerRegistrationData);
            if (newOwnerEntity.getUserId() == null) {
                newOwnerEntity.setUserId(UUID.randomUUID());
            }

            Owner persistedOwnerEntity = ownerRepository.save(newOwnerEntity);
            return transformToDataTransferObject(persistedOwnerEntity);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Database error during owner registration: " + e.getMessage());
            throw new RuntimeException("Failed to register new owner", e);
        }
    }

    // Updates owner details (validates email uniqueness, handles image uploads)
    public OwnerDTO modifyOwner(UUID targetOwnerId, OwnerDTO updatedOwnerData) {
        if (targetOwnerId == null) {
            throw new IllegalArgumentException("The Owner ID parameter must not be null.");
        }
        if (updatedOwnerData == null) {
            throw new IllegalArgumentException("Updated owner data must not be null.");
        }

        try {
            return ownerRepository.findById(targetOwnerId).map(existingOwner -> {
                if (updatedOwnerData.getEmail() != null && !updatedOwnerData.getEmail().equals(existingOwner.getEmail())) {
                    if (ownerRepository.findByEmail(updatedOwnerData.getEmail()).isPresent()) {
                        throw new IllegalStateException("Email is already in use by another owner.");
                    }
                }

                if (updatedOwnerData.getOwnerCode() != null) {
                    existingOwner.setOwnerCode(updatedOwnerData.getOwnerCode());
                }
                if (updatedOwnerData.getCompanyName() != null) {
                    existingOwner.setCompanyName(updatedOwnerData.getCompanyName());
                }
                if (updatedOwnerData.getCompanyEmail() != null) {
                    existingOwner.setCompanyEmail(updatedOwnerData.getCompanyEmail());
                }
                if (updatedOwnerData.getCompanyNumber() != null) {
                    existingOwner.setCompanyNumber(updatedOwnerData.getCompanyNumber());
                }
                
                if (updatedOwnerData.getFacebookUrl() != null) {
                    existingOwner.setFacebookUrl(updatedOwnerData.getFacebookUrl());
                }
                if (updatedOwnerData.getTwitterUrl() != null) {
                    existingOwner.setTwitterUrl(updatedOwnerData.getTwitterUrl());
                }
                if (updatedOwnerData.getInstagramUrl() != null) {
                    existingOwner.setInstagramUrl(updatedOwnerData.getInstagramUrl());
                }
                
                if (updatedOwnerData.getBannerImageUrl() != null && !updatedOwnerData.getBannerImageUrl().equals(existingOwner.getBannerImageUrl())) {
                    String uploadedUrl = imageKitService.uploadImage(updatedOwnerData.getBannerImageUrl(), AppConstants.OWNER_BANNER_PREFIX + existingOwner.getUserId());
                    existingOwner.setBannerImageUrl(uploadedUrl);
                }
                
                if (updatedOwnerData.getFullName() != null) {
                    existingOwner.setFullName(updatedOwnerData.getFullName());
                }
                if (updatedOwnerData.getEmail() != null) {
                    existingOwner.setEmail(updatedOwnerData.getEmail());
                }
                if (updatedOwnerData.getPhone() != null) {
                    existingOwner.setPhone(updatedOwnerData.getPhone());
                }
                
                if (updatedOwnerData.getProfilePictureUrl() != null && !updatedOwnerData.getProfilePictureUrl().equals(existingOwner.getProfilePictureUrl())) {
                    String uploadedUrl = imageKitService.uploadImage(updatedOwnerData.getProfilePictureUrl(), AppConstants.OWNER_PROFILE_PREFIX + existingOwner.getUserId());
                    existingOwner.setProfilePictureUrl(uploadedUrl);
                }
                
                if (updatedOwnerData.getStatus() != null) {
                    existingOwner.setStatus(updatedOwnerData.getStatus());
                }
                
                Owner successfullyUpdatedEntity = ownerRepository.save(existingOwner);
                return transformToDataTransferObject(successfullyUpdatedEntity);
            }).orElse(null);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR during owner modification: " + e.getMessage());
            throw new RuntimeException("Failed to update owner details", e);
        }
    }

    // Deletes owner from database
    public void removeOwner(UUID targetOwnerId) {
        if (targetOwnerId == null) {
            throw new IllegalArgumentException("The Owner ID parameter must not be null.");
        }
        try {
            if (!ownerRepository.existsById(targetOwnerId)) {
                throw new IllegalStateException("Cannot delete owner because no owner was found with ID: " + targetOwnerId);
            }
            ownerRepository.deleteById(targetOwnerId);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Database error during owner deletion: " + e.getMessage());
            throw new RuntimeException("Failed to delete owner", e);
        }
    }

    // Converts Owner entity to OwnerDTO (maintains clean API contract)
    private OwnerDTO transformToDataTransferObject(Owner sourceOwnerEntity) {
        if (sourceOwnerEntity == null) {
            return null;
        }

        OwnerDTO resultantDto = new OwnerDTO();
        BeanUtils.copyProperties(sourceOwnerEntity, resultantDto);

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

    // Converts OwnerDTO to Owner entity (for database persistence)
    private Owner transformToDatabaseEntity(OwnerDTO sourceOwnerData) {
        if (sourceOwnerData == null) {
            return null;
        }
        Owner resultantEntity = new Owner();
        BeanUtils.copyProperties(sourceOwnerData, resultantEntity);
        return resultantEntity;
    }
}
