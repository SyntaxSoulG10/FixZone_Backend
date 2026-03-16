package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.ServicePackageDTO;
import com.fixzone.fixzon_backend.model.ServicePackage;
import com.fixzone.fixzon_backend.repository.ServicePackageRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ServicePackageService {

    private final ServicePackageRepository repository;

    public ServicePackageService(ServicePackageRepository repository) {
        this.repository = repository;
    }

    public List<ServicePackageDTO> getAllPackages() {
        return repository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ServicePackageDTO> getPackagesByCenter(UUID centerId) {
        return repository.findByCenterId(centerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ServicePackageDTO getPackageById(UUID id) {
        return repository.findById(id)
                .map(this::convertToDTO)
                .orElse(null);
    }

    public ServicePackageDTO createPackage(ServicePackageDTO dto) {
        ServicePackage model = new ServicePackage();
        BeanUtils.copyProperties(dto, model, "packageId", "createdAt");
        ServicePackage saved = repository.save(model);
        return convertToDTO(saved);
    }

    public ServicePackageDTO updatePackage(UUID id, ServicePackageDTO dto) {
        return repository.findById(id).map(existing -> {
            BeanUtils.copyProperties(dto, existing, "packageId", "centerId", "createdAt");
            return convertToDTO(repository.save(existing));
        }).orElse(null);
    }

    public void deletePackage(UUID id) {
        repository.deleteById(id);
    }

    private ServicePackageDTO convertToDTO(ServicePackage model) {
        ServicePackageDTO dto = new ServicePackageDTO();
        BeanUtils.copyProperties(model, dto);
        return dto;
    }
}
