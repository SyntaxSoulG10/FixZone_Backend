package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.OwnerDTO;
import com.fixzone.fixzon_backend.config.AppConstants;
import com.fixzone.fixzon_backend.model.Owner;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
import com.fixzone.fixzon_backend.repository.NotificationRepository;
import com.fixzone.fixzon_backend.repository.SubscriptionBillingRepository;
import com.fixzone.fixzon_backend.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for managing Company Owner profiles.
 * Handles the lifecycle of owner data, including registration, updates, and retrieval.
 */
@Service
public class OwnerService {
    private static final Logger log = LoggerFactory.getLogger(OwnerService.class);

    private final OwnerRepository ownerRepository;
    private final ImageKitService imageKitService;
    private final NotificationRepository notificationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionBillingRepository subscriptionBillingRepository;

    /**
     * Constructor injection for required dependencies.
     */
    public OwnerService(OwnerRepository ownerRepository, 
                        ImageKitService imageKitService,
                        NotificationRepository notificationRepository,
                        SubscriptionRepository subscriptionRepository,
                        SubscriptionBillingRepository subscriptionBillingRepository) {
        this.ownerRepository = ownerRepository;
        this.imageKitService = imageKitService;
        this.notificationRepository = notificationRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionBillingRepository = subscriptionBillingRepository;
    }

    /**
     * Retrieves all owners registered in the system.
     * Maps entities to DTOs to maintain a clean separation between DB and API layers.
     */
    public List<OwnerDTO> retrieveAllOwners() {
        try {
            return ownerRepository.findAll().stream()
                .map(this::transformToDataTransferObject)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Database error while retrieving owners: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve all owners", e);
        }
    }

    public OwnerDTO retrieveOwnerById(UUID targetOwnerId) {
        if (targetOwnerId == null) {
            throw new IllegalArgumentException("The Owner ID parameter must not be null.");
        }
        try {
            return ownerRepository.findById(targetOwnerId)
                .map(this::transformToDataTransferObject)
                .orElse(null);
        } catch (Exception e) {
            log.error("Database error while retrieving owner by ID: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve owner by ID", e);
        }
    }

    public OwnerDTO retrieveOwnerByCode(String ownerCode) {
        if (ownerCode == null || ownerCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner code must not be null or empty.");
        }
        try {
            return ownerRepository.findByOwnerCode(ownerCode)
                .map(this::transformToDataTransferObject)
                .orElse(null);
        } catch (Exception e) {
            log.error("Database error while retrieving owner by code: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve owner by code", e);
        }
    }

    public OwnerDTO retrieveOwnerByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email must not be null or empty.");
        }
        try {
            return ownerRepository.findByEmailIgnoreCase(email.trim())
                .or(() -> ownerRepository.findByEmail(email.trim()))
                .map(this::transformToDataTransferObject)
                .orElse(null);
        } catch (Exception e) {
            log.error("Database error while retrieving owner by email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve owner by email", e);
        }
    }

    /**
     * Registers a new owner. Generates a unique UUID if one isn't provided.
     */
    public OwnerDTO registerOwner(OwnerDTO newOwnerRegistrationData) {
        if (newOwnerRegistrationData == null) {
            throw new IllegalArgumentException("Registration data must not be null.");
        }
        if (newOwnerRegistrationData.getEmail() == null || newOwnerRegistrationData.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required for registration.");
        }

        try {
            // Check for existing email to avoid duplicates
            if (ownerRepository.findByEmail(newOwnerRegistrationData.getEmail()).isPresent()) {
                throw new IllegalStateException("An owner with this email already exists.");
            }

            // Transforms DTO to JPA Entity for repository operations.
            Owner newOwnerEntity = transformToDatabaseEntity(newOwnerRegistrationData);

            // Generates unique identifier for new entities.
            if (newOwnerEntity.getUserId() == null) {
                newOwnerEntity.setUserId(UUID.randomUUID());
            }

            Owner persistedOwnerEntity = ownerRepository.save(newOwnerEntity);
            return transformToDataTransferObject(persistedOwnerEntity);
        } catch (IllegalStateException e) {
            throw e; // Rethrow expected state exceptions
        } catch (Exception e) {
            log.error("Database error during owner registration: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to register new owner", e);
        }
    }

    /**
     * Updates an existing owner's details.
     * Explicitly maps fields to preserve inherited user properties and keeps login email immutable.
     */
    public OwnerDTO modifyOwner(UUID targetOwnerId, OwnerDTO updatedOwnerData) {
        if (targetOwnerId == null) {
            throw new IllegalArgumentException("The Owner ID parameter must not be null.");
        }
        if (updatedOwnerData == null) {
            throw new IllegalArgumentException("Updated owner data must not be null.");
        }

        try {
            return ownerRepository.findById(targetOwnerId).map(existingOwner -> {
                // SECURITY: Primary login email cannot be changed via profile update
                // (existingOwner.getEmail() is retained and protected)

                // 1. Company Name validation (Required, 2-150 chars)
                if (updatedOwnerData.getCompanyName() != null) {
                    String compName = updatedOwnerData.getCompanyName().trim();
                    if (compName.length() < 2 || compName.length() > 150) {
                        throw new IllegalArgumentException("Company name must be between 2 and 150 characters.");
                    }
                    existingOwner.setCompanyName(compName);
                }

                // 2. Company Email validation (Optional, valid format)
                if (updatedOwnerData.getCompanyEmail() != null) {
                    String compEmail = updatedOwnerData.getCompanyEmail().trim();
                    if (!compEmail.isEmpty()) {
                        if (!compEmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                            throw new IllegalArgumentException("Invalid company email format.");
                        }
                        existingOwner.setCompanyEmail(compEmail);
                    } else {
                        existingOwner.setCompanyEmail(null);
                    }
                }

                // 3. Company Phone validation (Optional, 9-20 chars)
                if (updatedOwnerData.getCompanyNumber() != null) {
                    String compNum = updatedOwnerData.getCompanyNumber().trim();
                    if (!compNum.isEmpty()) {
                        if (!compNum.matches("^[0-9+()\\s-]{9,20}$")) {
                            throw new IllegalArgumentException("Company phone number must be 9-20 digits.");
                        }
                        existingOwner.setCompanyNumber(compNum);
                    } else {
                        existingOwner.setCompanyNumber(null);
                    }
                }

                // 4. Social URLs sanitization
                if (updatedOwnerData.getFacebookUrl() != null) {
                    existingOwner.setFacebookUrl(sanitizeSocialUrl(updatedOwnerData.getFacebookUrl()));
                }
                if (updatedOwnerData.getTwitterUrl() != null) {
                    existingOwner.setTwitterUrl(sanitizeSocialUrl(updatedOwnerData.getTwitterUrl()));
                }
                if (updatedOwnerData.getInstagramUrl() != null) {
                    existingOwner.setInstagramUrl(sanitizeSocialUrl(updatedOwnerData.getInstagramUrl()));
                }

                // 5. Banner image update
                if (updatedOwnerData.getBannerImageUrl() != null && !updatedOwnerData.getBannerImageUrl().equals(existingOwner.getBannerImageUrl())) {
                    log.info("[OWNER] Detected change in Banner Image. Length: {}", updatedOwnerData.getBannerImageUrl().length());
                    String uploadedUrl = imageKitService.uploadImage(updatedOwnerData.getBannerImageUrl(), AppConstants.OWNER_BANNER_PREFIX + existingOwner.getUserId());
                    existingOwner.setBannerImageUrl(uploadedUrl);
                    log.info("[OWNER] Banner updated to: {}", uploadedUrl);
                }

                // 6. Full Name validation (Required, 2-100 chars)
                if (updatedOwnerData.getFullName() != null) {
                    String fName = updatedOwnerData.getFullName().trim();
                    if (fName.length() < 2 || fName.length() > 100) {
                        throw new IllegalArgumentException("Full name must be between 2 and 100 characters.");
                    }
                    existingOwner.setFullName(fName);
                }

                // 7. Owner Personal Phone validation (Optional, 9-20 chars)
                if (updatedOwnerData.getPhone() != null) {
                    String userPhone = updatedOwnerData.getPhone().trim();
                    if (!userPhone.isEmpty()) {
                        if (!userPhone.matches("^[0-9+()\\s-]{9,20}$")) {
                            throw new IllegalArgumentException("Personal phone number must be 9-20 digits.");
                        }
                        existingOwner.setPhone(userPhone);
                    } else {
                        existingOwner.setPhone(null);
                    }
                }

                // 8. Profile picture update
                if (updatedOwnerData.getProfilePictureUrl() != null && !updatedOwnerData.getProfilePictureUrl().equals(existingOwner.getProfilePictureUrl())) {
                    log.info("[OWNER] Detected change in Profile Picture. Length: {}", updatedOwnerData.getProfilePictureUrl().length());
                    String uploadedUrl = imageKitService.uploadImage(updatedOwnerData.getProfilePictureUrl(), AppConstants.OWNER_PROFILE_PREFIX + existingOwner.getUserId());
                    existingOwner.setProfilePictureUrl(uploadedUrl);
                    log.info("[OWNER] Profile picture updated to: {}", uploadedUrl);
                }

                if (updatedOwnerData.getStatus() != null) {
                    existingOwner.setStatus(updatedOwnerData.getStatus());
                }

                Owner successfullyUpdatedEntity = ownerRepository.save(existingOwner);
                return transformToDataTransferObject(successfullyUpdatedEntity);
            }).orElse(null);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e; // Rethrow validation exceptions so Spring returns 400 Bad Request
        } catch (Exception e) {
            // Logs critical errors during modification
            log.error("CRITICAL ERROR during owner modification: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update owner details", e);
        }
    }

    private String sanitizeSocialUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        String trimmed = url.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            return "https://" + trimmed;
        }
        return trimmed;
    }

    @Transactional
    public void removeOwner(UUID targetOwnerId) {
        if (targetOwnerId == null) {
            throw new IllegalArgumentException("The Owner ID parameter must not be null.");
        }
        try {
            if (!ownerRepository.existsById(targetOwnerId)) {
                throw new IllegalStateException("Cannot delete owner because no owner was found with ID: " + targetOwnerId);
            }
            // 1. Delete notifications sent to this owner
            if (notificationRepository != null) {
                notificationRepository.deleteByRecipientUserId(targetOwnerId);
            }

            // 2. Delete subscription and associated billing records if present
            if (subscriptionRepository != null) {
                subscriptionRepository.findByOwnerUserId(targetOwnerId).ifPresent(sub -> {
                    if (subscriptionBillingRepository != null) {
                        subscriptionBillingRepository.deleteBySubscriptionId(sub.getId());
                    }
                    subscriptionRepository.delete(sub);
                });
            }

            // 3. Delete owner record
            ownerRepository.deleteById(targetOwnerId);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Database error during owner deletion: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete owner", e);
        }
    }

    /**
     * Converts entity to DTO to protect internal database structure.
     */
    private OwnerDTO transformToDataTransferObject(Owner sourceOwnerEntity) {
        if (sourceOwnerEntity == null) {
            return null;
        }

        OwnerDTO resultantDto = new OwnerDTO();
        BeanUtils.copyProperties(sourceOwnerEntity, resultantDto);

        // Maps inherited fields for frontend consistency
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
        if (sourceOwnerData == null) {
            return null;
        }
        Owner resultantEntity = new Owner();
        BeanUtils.copyProperties(sourceOwnerData, resultantEntity);
        return resultantEntity;
    }
}
