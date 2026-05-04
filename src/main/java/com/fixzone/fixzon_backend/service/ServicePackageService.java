package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.ServicePackageDTO;
import com.fixzone.fixzon_backend.model.ServicePackage;
import com.fixzone.fixzon_backend.repository.ServicePackageRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.util.List;

import java.util.UUID;
import java.util.stream.Collectors;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;

import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional // All methods in this service participate in database transactions
public class ServicePackageService {

    // Repository for service package database operations
    private final ServicePackageRepository repository;
    
    // Repository for service center lookups (to validate center exists)
    private final ServiceCenterRepository centerRepository;

    // Constructor-based dependency injection for immutable fields
    public ServicePackageService(ServicePackageRepository repository, ServiceCenterRepository centerRepository) {
        this.repository = repository;
        this.centerRepository = centerRepository;
    }

    /**
     * Retrieves all active service packages
     * @return List of ServicePackageDTOs that are marked as active
     * @throws RuntimeException if database error occurs
     */
    @Transactional(readOnly = true) // Read-only transaction optimizes database operations
    public List<ServicePackageDTO> getAllPackages() {
        try {
            // Fetch only active packages and convert to DTOs
            return repository.findByIsActiveTrue().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // Log error and throw exception for proper error handling
            System.err.println("Database error while retrieving packages: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve packages", e);
        }
    }

    /**
     * Retrieves all active service packages for a specific service center
     * @param centerId UUID of the service center
     * @return List of ServicePackageDTOs for the specified center
     * @throws IllegalArgumentException if centerId is null
     * @throws RuntimeException if database error occurs
     */
    @Transactional(readOnly = true)
    public List<ServicePackageDTO> getPackagesByCenter(UUID centerId) {
        // Validate input parameter
        if (centerId == null) {
            throw new IllegalArgumentException("Center ID cannot be null");
        }
        try {
            // Query packages by center ID and active status
            return repository.findByServiceCenter_CenterIdAndIsActiveTrue(centerId).stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Database error while retrieving packages by center: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve packages by center", e);
        }
    }

    /**
     * Retrieves service packages by owner email address
     * @param email Email of the service center owner
     * @return List of ServicePackageDTOs owned by the specified email
     * @throws IllegalArgumentException if email is null or empty
     * @throws RuntimeException if database error occurs
     */
    @Transactional(readOnly = true)
    public List<ServicePackageDTO> getPackagesByOwnerEmail(String email) {
        // Validate email parameter
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        try {
            // Query packages using custom repository method
            return repository.findPackagesByOwnerEmail(email).stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Database error while retrieving packages by owner email: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve packages by owner email", e);
        }
    }

    /**
     * Retrieves service packages by owner code
     * @param code Unique owner code identifier
     * @return List of ServicePackageDTOs owned by the specified code
     * @throws IllegalArgumentException if code is null or empty
     * @throws RuntimeException if database error occurs
     */
    @Transactional(readOnly = true)
    public List<ServicePackageDTO> getPackagesByOwnerCode(String code) {
        // Validate owner code parameter
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner code cannot be null or empty");
        }
        try {
            // Query packages using custom repository method
            return repository.findPackagesByOwnerCode(code).stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Database error while retrieving packages by owner code: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve packages by owner code", e);
        }
    }

    /**
     * Retrieves a single service package by its ID
     * @param id UUID of the service package
     * @return ServicePackageDTO if found, null otherwise
     * @throws IllegalArgumentException if id is null
     * @throws RuntimeException if database error occurs
     */
    @Transactional(readOnly = true)
    public ServicePackageDTO getPackageById(UUID id) {
        // Validate ID parameter
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
        try {
            // Find by ID and convert to DTO, return null if not found
            return repository.findById(id)
                    .map(this::convertToDTO)
                    .orElse(null);
        } catch (Exception e) {
            System.err.println("Database error while retrieving package by ID: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve package by ID", e);
        }
    }

    /**
     * Creates a new service package
     * @param dto ServicePackageDTO containing package information
     * @return Created ServicePackageDTO with generated ID
     * @throws IllegalArgumentException if dto is null
     * @throws RuntimeException if center not found or database error occurs
     */
    public ServicePackageDTO createPackage(ServicePackageDTO dto) {
        // Validate input
        if (dto == null) {
            throw new IllegalArgumentException("Service Package data cannot be null");
        }
        try {
            // Create new entity and copy properties from DTO
            // Exclude packageId and createdAt as they are auto-generated
            ServicePackage model = new ServicePackage();
            BeanUtils.copyProperties(dto, model, "packageId", "createdAt");
            
            // Link to service center if provided
            if (dto.getCenterId() != null) {
                // Validate that service center exists
                model.setServiceCenter(centerRepository.findById(dto.getCenterId())
                    .orElseThrow(() -> new RuntimeException("Service Center not found with id: " + dto.getCenterId())));
            }
            
            // Save to database and convert back to DTO
            ServicePackage saved = repository.save(model);
            return convertToDTO(saved);
        } catch (RuntimeException e) {
            // Re-throw runtime exceptions as-is
            throw e;
        } catch (Exception e) {
            System.err.println("Database error while creating service package: " + e.getMessage());
            throw new RuntimeException("Failed to create service package", e);
        }
    }

    /**
     * Updates an existing service package
     * @param id UUID of the package to update
     * @param dto ServicePackageDTO with updated information
     * @return Updated ServicePackageDTO, null if package not found
     * @throws IllegalArgumentException if id or dto is null
     * @throws RuntimeException if center not found or database error occurs
     */
    public ServicePackageDTO updatePackage(UUID id, ServicePackageDTO dto) {
        // Validate input parameters
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Service Package data cannot be null");
        }
        try {
            // Fetch existing package
            ServicePackage existing = repository.findById(id).orElse(null);
            
            // Update if found
            if (existing != null) {
                // Copy properties from DTO, excluding packageId and createdAt
                BeanUtils.copyProperties(dto, existing, "packageId", "createdAt");
                
                // Update service center reference if provided
                if (dto.getCenterId() != null) {
                    existing.setServiceCenter(centerRepository.findById(dto.getCenterId())
                        .orElseThrow(() -> new RuntimeException("Service Center not found with id: " + dto.getCenterId())));
                }
                // Save updated package and return as DTO
                return convertToDTO(repository.save(existing));
            }
            return null; // Return null if package not found
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Database error while updating service package: " + e.getMessage());
            throw new RuntimeException("Failed to update service package", e);
        }
    }

    /**
     * Deletes a service package by ID
     * @param id UUID of the package to delete
     * @throws IllegalArgumentException if id is null
     * @throws IllegalStateException if package not found
     * @throws RuntimeException if database error occurs
     */
    public void deletePackage(UUID id) {
        // Validate ID parameter
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
        try {
            // Check if package exists before attempting delete
            if (!repository.existsById(id)) {
                throw new IllegalStateException("Service package not found with id: " + id);
            }
            // Delete package from database
            repository.deleteById(id);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Database error while deleting service package: " + e.getMessage());
            throw new RuntimeException("Failed to delete service package", e);
        }
    }

    /**
     * Converts ServicePackage entity to ServicePackageDTO
     * @param model ServicePackage entity from database
     * @return ServicePackageDTO with all fields populated
     * @throws IllegalArgumentException if model is null
     */
    private ServicePackageDTO convertToDTO(ServicePackage model) {
        // Validate input
        if (model == null) {
            throw new IllegalArgumentException("ServicePackage model must not be null");
        }
        // Create DTO and copy all properties
        ServicePackageDTO dto = new ServicePackageDTO();
        BeanUtils.copyProperties(model, dto);
        
        // Map related service center ID
        if (model.getServiceCenter() != null) {
            dto.setCenterId(model.getServiceCenter().getCenterId());
        }
        return dto;
    }
}
