package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.ServicePackageDTO;
import com.fixzone.fixzon_backend.model.ServicePackage;
import com.fixzone.fixzon_backend.repository.ServicePackageRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ServicePackageService {

    private final ServicePackageRepository servicePackageRepository;

    public ServicePackageService(ServicePackageRepository servicePackageRepository) {
        this.servicePackageRepository = servicePackageRepository;
    }

    public List<ServicePackageDTO> getAllServicePackages() {
        return servicePackageRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private ServicePackageDTO convertToDTO(ServicePackage servicePackage) {
        return new ServicePackageDTO(
                servicePackage.getPackageId(),
                servicePackage.getServiceName(),
                servicePackage.getPrice(),
                servicePackage.getDescription(),
                servicePackage.getOwner() != null ? servicePackage.getOwner().getCompanyName() : null
        );
    }
}

