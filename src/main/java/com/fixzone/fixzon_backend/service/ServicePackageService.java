package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.ServicePackageDTO;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.model.ServicePackage;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.ServicePackageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ServicePackageService {

    @Autowired
    private ServicePackageRepository servicePackageRepository;

    @Autowired
    private ServiceCenterRepository serviceCenterRepository;

    public List<ServicePackageDTO> getAllServicePackages() {
        return servicePackageRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<ServicePackageDTO> getServicePackagesByCenter(UUID centerId) {
        return servicePackageRepository.findAll().stream()
                .filter(pkg -> pkg.getServiceCenter().getCenterId().equals(centerId))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public ServicePackageDTO getServicePackageById(UUID packageId) {
        return servicePackageRepository.findById(packageId)
                .map(this::mapToDTO)
                .orElse(null);
    }

    public ServicePackageDTO createServicePackage(ServicePackageDTO servicePackageDTO) {
        ServicePackage servicePackage = mapToEntity(servicePackageDTO);
        if (servicePackage.getPackageId() == null) {
            servicePackage.setPackageId(UUID.randomUUID());
        }
        servicePackage.setCreatedAt(LocalDateTime.now());
        servicePackage.setCreatedBy("system"); // Placeholder
        ServicePackage savedPackage = servicePackageRepository.save(servicePackage);
        return mapToDTO(savedPackage);
    }

    public ServicePackageDTO updateServicePackage(UUID packageId, ServicePackageDTO servicePackageDTO) {
        if (servicePackageRepository.existsById(packageId)) {
            ServicePackage servicePackage = mapToEntity(servicePackageDTO);
            servicePackage.setPackageId(packageId);
            servicePackage.setUpdatedAt(LocalDateTime.now());
            servicePackage.setUpdatedBy("system"); // Placeholder
            ServicePackage updatedPackage = servicePackageRepository.save(servicePackage);
            return mapToDTO(updatedPackage);
        }
        return null;
    }

    public boolean deleteServicePackage(UUID packageId) {
        if (servicePackageRepository.existsById(packageId)) {
            servicePackageRepository.deleteById(packageId);
            return true;
        }
        return false;
    }

    private ServicePackageDTO mapToDTO(ServicePackage servicePackage) {
        return new ServicePackageDTO(
                servicePackage.getPackageId(),
                servicePackage.getServiceCenter().getCenterId(),
                servicePackage.getName(),
                servicePackage.getType(),
                servicePackage.getDescription(),
                servicePackage.getBasePrice(),
                servicePackage.getEstimatedDurationMins(),
                servicePackage.getIsActive());
    }

    private ServicePackage mapToEntity(ServicePackageDTO dto) {
        ServicePackage entity = new ServicePackage();
        entity.setPackageId(dto.getPackageId());

        ServiceCenter center = serviceCenterRepository.findById(dto.getCenterId())
                .orElseThrow(() -> new RuntimeException("Service Center not found with id: " + dto.getCenterId()));

        entity.setServiceCenter(center);
        entity.setName(dto.getName());
        entity.setType(dto.getType());
        entity.setDescription(dto.getDescription());
        entity.setBasePrice(dto.getBasePrice());
        entity.setEstimatedDurationMins(dto.getEstimatedDurationMins());
        entity.setIsActive(dto.getIsActive());

        return entity;
    }
}
