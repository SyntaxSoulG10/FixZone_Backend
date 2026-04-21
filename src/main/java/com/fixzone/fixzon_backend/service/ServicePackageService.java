package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.ServicePackageDTO;
import com.fixzone.fixzon_backend.model.ServicePackage;
import com.fixzone.fixzon_backend.repository.ServicePackageRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.Optional;
import java.util.stream.Collectors;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;

import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ServicePackageService {

    private final ServicePackageRepository repository;
    private final ServiceCenterRepository centerRepository;

    public ServicePackageService(ServicePackageRepository repository, ServiceCenterRepository centerRepository) {
        this.repository = repository;
        this.centerRepository = centerRepository;
    }

    public List<ServicePackageDTO> getAllPackages() {
        return repository.findByIsActiveTrue().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ServicePackageDTO> getPackagesByCenter(UUID centerId) {
        return repository.findByServiceCenter_CenterIdAndIsActiveTrue(centerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ServicePackageDTO getPackageById(UUID id) {
        Objects.requireNonNull(id, "ID must not be null");
        return repository.findById(id)
                .map(this::convertToDTO)
                .orElse(null);
    }

    public ServicePackageDTO createPackage(ServicePackageDTO dto) {
        ServicePackage model = new ServicePackage();
        BeanUtils.copyProperties(Objects.requireNonNull(dto), model, "packageId", "createdAt");
        
        if (dto.getCenterId() != null) {
            model.setServiceCenter(centerRepository.findById(Objects.requireNonNull(dto.getCenterId()))
                .orElseThrow(() -> new RuntimeException("Service Center not found")));
        }
        
        ServicePackage saved = Objects.requireNonNull(repository.save(model));
        return convertToDTO(saved);
    }

    public ServicePackageDTO updatePackage(UUID id, ServicePackageDTO dto) {
        Objects.requireNonNull(id, "ID must not be null");
        Optional<ServicePackage> optionalPackage = repository.findById(id);
        if (optionalPackage.isPresent()) {
            ServicePackage existing = optionalPackage.get();
            if (dto != null) {
                BeanUtils.copyProperties(dto, existing, "packageId", "createdAt");
                if (dto.getCenterId() != null) {
                    existing.setServiceCenter(centerRepository.findById(Objects.requireNonNull(dto.getCenterId()))
                        .orElseThrow(() -> new RuntimeException("Service Center not found")));
                }
            }
            ServicePackage saved = repository.save(existing);
            return convertToDTO(saved);
        }
        return null;
    }

    public void deletePackage(UUID id) {
        Objects.requireNonNull(id, "ID must not be null");
        repository.deleteById(id);
    }

    private ServicePackageDTO convertToDTO(ServicePackage model) {
        Objects.requireNonNull(model, "ServicePackage model must not be null");
        ServicePackageDTO dto = new ServicePackageDTO();
        BeanUtils.copyProperties(model, dto);
        if (model.getServiceCenter() != null) {
            dto.setCenterId(model.getServiceCenter().getCenterId());
        }
        return dto;
    }
}
