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
@Transactional
public class ServicePackageService {

    private final ServicePackageRepository repository;
    private final ServiceCenterRepository centerRepository;

    public ServicePackageService(ServicePackageRepository repository, ServiceCenterRepository centerRepository) {
        this.repository = repository;
        this.centerRepository = centerRepository;
    }

    @Transactional(readOnly = true)
    public List<ServicePackageDTO> getAllPackages() {
        try {
            return repository.findByIsActiveTrue().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Database error while retrieving packages: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve packages", e);
        }
    }

    @Transactional(readOnly = true)
    public List<ServicePackageDTO> getPackagesByCenter(UUID centerId) {
        if (centerId == null) {
            throw new IllegalArgumentException("Center ID cannot be null");
        }
        try {
            return repository.findByServiceCenter_CenterIdAndIsActiveTrue(centerId).stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Database error while retrieving packages by center: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve packages by center", e);
        }
    }

    @Transactional(readOnly = true)
    public List<ServicePackageDTO> getPackagesByOwnerEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        try {
            return repository.findPackagesByOwnerEmail(email).stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Database error while retrieving packages by owner email: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve packages by owner email", e);
        }
    }

    @Transactional(readOnly = true)
    public List<ServicePackageDTO> getPackagesByOwnerCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner code cannot be null or empty");
        }
        try {
            return repository.findPackagesByOwnerCode(code).stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Database error while retrieving packages by owner code: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve packages by owner code", e);
        }
    }

    @Transactional(readOnly = true)
    public ServicePackageDTO getPackageById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
        try {
            return repository.findById(id)
                    .map(this::convertToDTO)
                    .orElse(null);
        } catch (Exception e) {
            System.err.println("Database error while retrieving package by ID: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve package by ID", e);
        }
    }

    public ServicePackageDTO createPackage(ServicePackageDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Service Package data cannot be null");
        }
        try {
            ServicePackage model = new ServicePackage();
            BeanUtils.copyProperties(dto, model, "packageId", "createdAt");
            
            if (dto.getCenterId() != null) {
                model.setServiceCenter(centerRepository.findById(dto.getCenterId())
                    .orElseThrow(() -> new RuntimeException("Service Center not found with id: " + dto.getCenterId())));
            }
            
            ServicePackage saved = repository.save(model);
            return convertToDTO(saved);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Database error while creating service package: " + e.getMessage());
            throw new RuntimeException("Failed to create service package", e);
        }
    }

    public ServicePackageDTO updatePackage(UUID id, ServicePackageDTO dto) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Service Package data cannot be null");
        }
        try {
            ServicePackage existing = repository.findById(id).orElse(null);
            
            if (existing != null) {
                BeanUtils.copyProperties(dto, existing, "packageId", "createdAt");
                if (dto.getCenterId() != null) {
                    existing.setServiceCenter(centerRepository.findById(dto.getCenterId())
                        .orElseThrow(() -> new RuntimeException("Service Center not found with id: " + dto.getCenterId())));
                }
                return convertToDTO(repository.save(existing));
            }
            return null;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Database error while updating service package: " + e.getMessage());
            throw new RuntimeException("Failed to update service package", e);
        }
    }

    public void deletePackage(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
        try {
            if (!repository.existsById(id)) {
                throw new IllegalStateException("Service package not found with id: " + id);
            }
            repository.deleteById(id);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Database error while deleting service package: " + e.getMessage());
            throw new RuntimeException("Failed to delete service package", e);
        }
    }

    private ServicePackageDTO convertToDTO(ServicePackage model) {
        if (model == null) {
            throw new IllegalArgumentException("ServicePackage model must not be null");
        }
        ServicePackageDTO dto = new ServicePackageDTO();
        BeanUtils.copyProperties(model, dto);
        if (model.getServiceCenter() != null) {
            dto.setCenterId(model.getServiceCenter().getCenterId());
        }
        return dto;
    }
}
