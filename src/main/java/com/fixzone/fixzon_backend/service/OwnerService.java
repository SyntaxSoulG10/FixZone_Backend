package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.OwnerDTO;
import com.fixzone.fixzon_backend.model.Owner;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

// The @Service annotation registers this class as a Spring component.
// We use a service layer to contain all the business logic, isolating the database and the controller layers.
@Service
public class OwnerService {

    private final OwnerRepository ownerRepository;

    // Constructor injection is preferred over field injection (@Autowired on the field).
    // This allows the dependencies to be mocking-friendly for testing.
    @Autowired
    public OwnerService(OwnerRepository ownerRepository) {
        this.ownerRepository = ownerRepository;
    }

    public List<OwnerDTO> retrieveAllOwners() {
        // We retrieve all entities and map them to DTOs so that sensitive entity properties
        // do not accidentally leak out to the frontend application.
        return ownerRepository.findAll().stream()
                .map(this::transformToDataTransferObject)
                .collect(Collectors.toList());
    }

    public OwnerDTO retrieveOwnerById(UUID targetOwnerId) {
        // Enforce non-null constraints before interacting with the database
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

    public OwnerDTO modifyOwner(UUID targetOwnerId, OwnerDTO updatedOwnerData) {
        Objects.requireNonNull(targetOwnerId, "The Owner ID parameter must not be null.");
        
        // We check existence first; updating a non-existent entity could incorrectly create a new one.
        if (ownerRepository.existsById(targetOwnerId)) {
            Owner ownerEntityToUpdate = transformToDatabaseEntity(updatedOwnerData);
            
            if (ownerEntityToUpdate != null) {
                // Ensure the entity updates the specific record rather than inserting a new one
                ownerEntityToUpdate.setUserId(targetOwnerId);
                Owner successfullyUpdatedEntity = ownerRepository.save(ownerEntityToUpdate);
                return transformToDataTransferObject(successfullyUpdatedEntity);
            }
        }
        return null; // Signals to the controller that the entity was not found
    }

    public void removeOwner(UUID targetOwnerId) {
        Objects.requireNonNull(targetOwnerId, "The Owner ID parameter must not be null.");
        // Deleting directly by ID is faster than fetching the entity first
        ownerRepository.deleteById(targetOwnerId);
    }

    // Extracted helper method for separating mapping concerns. Transforms Entity to DTO.
    private OwnerDTO transformToDataTransferObject(Owner sourceOwnerEntity) {
        if (sourceOwnerEntity == null) {
            return null;
        }
        
        OwnerDTO resultantDto = new OwnerDTO();
        BeanUtils.copyProperties(sourceOwnerEntity, resultantDto);
        return resultantDto;
    }

    // Extracted helper method for separating mapping concerns. Transforms DTO to Entity.
    private Owner transformToDatabaseEntity(OwnerDTO sourceOwnerData) {
        if (sourceOwnerData == null) {
            return null;
        }
        
        Owner resultantEntity = new Owner();
        BeanUtils.copyProperties(sourceOwnerData, resultantEntity);
        return resultantEntity;
    }
}
