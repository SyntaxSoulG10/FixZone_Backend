package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.ServiceCenterDTO;
import com.fixzone.fixzon_backend.DTO.ServicePackageDTO;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.model.User;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.ServicePackageRepository;
import com.fixzone.fixzon_backend.repository.UserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ServiceCenterService {

    private final ServiceCenterRepository serviceCenterRepository;
    private final UserRepository userRepository;
    private final ServicePackageRepository servicePackageRepository;

    public ServiceCenterService(ServiceCenterRepository serviceCenterRepository, 
                               UserRepository userRepository,
                               ServicePackageRepository servicePackageRepository) {
        this.serviceCenterRepository = serviceCenterRepository;
        this.userRepository = userRepository;
        this.servicePackageRepository = servicePackageRepository;
    }

    public List<ServiceCenterDTO> getAllServiceCenters() {
        return serviceCenterRepository.findByIsActive(true).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ServiceCenterDTO getServiceCenterById(UUID id) {
        Objects.requireNonNull(id, "ID must not be null");
        ServiceCenter center = serviceCenterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service center not found with id: " + id));
        return convertToDTO(center);
    }

    public ServiceCenterDTO createServiceCenter(ServiceCenterDTO dto) {
        ServiceCenter center = convertToEntity(dto);
        if (center.getCenterId() == null) {
            center.setCenterId(UUID.randomUUID());
        }
        return convertToDTO(serviceCenterRepository.save(center));
    }

    public ServiceCenterDTO updateServiceCenter(UUID id, ServiceCenterDTO dto) {
        Objects.requireNonNull(id, "ID must not be null");
        ServiceCenter existingCenter = serviceCenterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service center not found with id: " + id));

        existingCenter.setName(dto.getName());
        existingCenter.setAddress(dto.getAddress());
        existingCenter.setContactPhone(dto.getContactPhone());
        existingCenter.setOpeningHours(dto.getOpeningHours());
        existingCenter.setRating(dto.getRating());
        existingCenter.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : existingCenter.getIsActive());
        existingCenter.setUpdatedBy(dto.getUpdatedBy());
        existingCenter.setSupportedVehicleBrands(dto.getSupportedVehicleBrands());

        return convertToDTO(serviceCenterRepository.save(existingCenter));
    }

    public void deleteServiceCenter(UUID id) {
        Objects.requireNonNull(id, "ID must not be null");
        serviceCenterRepository.deleteById(id);
    }

    private ServiceCenterDTO convertToDTO(ServiceCenter center) {
        if (center == null) return null;
        
        ServiceCenterDTO dto = new ServiceCenterDTO();
        BeanUtils.copyProperties(center, dto);
        
        if (center.getOwner() != null) {
            dto.setOwnerId(center.getOwner().getUserId());
        }

        // Populate active packages for this center
        List<ServicePackageDTO> packages = servicePackageRepository.findByServiceCenter_CenterIdAndIsActiveTrue(center.getCenterId())
                .stream()
                .map(pkg -> {
                    ServicePackageDTO pkgDto = new ServicePackageDTO();
                    BeanUtils.copyProperties(Objects.requireNonNull(pkg), pkgDto);
                    pkgDto.setCenterId(center.getCenterId()); // Manually set the UUID for the DTO
                    return pkgDto;
                })
                .collect(Collectors.toList());
        dto.setServicePackages(packages);

        return dto;
    }

    private ServiceCenter convertToEntity(ServiceCenterDTO dto) {
        ServiceCenter center = new ServiceCenter();
        center.setCenterId(dto.getCenterId());

        if (dto != null && dto.getOwnerId() != null) {
            User owner = userRepository.findById(dto.getOwnerId())
                    .orElseThrow(() -> new RuntimeException("Owner not found with id: " + dto.getOwnerId()));
            center.setOwner(Objects.requireNonNull(owner));
        }

        center.setName(dto.getName());
        center.setAddress(dto.getAddress());
        center.setContactPhone(dto.getContactPhone());
        center.setOpeningHours(dto.getOpeningHours());
        center.setRating(dto.getRating());
        center.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        center.setCreatedBy(dto.getCreatedBy());
        center.setUpdatedBy(dto.getUpdatedBy());
        center.setSupportedVehicleBrands(dto.getSupportedVehicleBrands());
        return center;
    }
}
