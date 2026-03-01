package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.ServiceCenterDTO;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.model.User;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ServiceCenterService {

    private final ServiceCenterRepository serviceCenterRepository;
    private final UserRepository userRepository;

    public ServiceCenterService(ServiceCenterRepository serviceCenterRepository, UserRepository userRepository) {
        this.serviceCenterRepository = serviceCenterRepository;
        this.userRepository = userRepository;
    }

    public List<ServiceCenterDTO> getAllServiceCenters() {
        return serviceCenterRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ServiceCenterDTO getServiceCenterById(UUID id) {
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
        serviceCenterRepository.deleteById(id);
    }

    private ServiceCenterDTO convertToDTO(ServiceCenter center) {
        return new ServiceCenterDTO(
                center.getCenterId(),
                center.getOwner() != null ? center.getOwner().getUserId() : null,
                center.getName(),
                center.getAddress(),
                center.getContactPhone(),
                center.getOpeningHours(),
                center.getRating(),
                center.getIsActive(),
                center.getCreatedAt(),
                center.getCreatedBy(),
                center.getUpdatedAt(),
                center.getUpdatedBy(),
                center.getSupportedVehicleBrands());
    }

    private ServiceCenter convertToEntity(ServiceCenterDTO dto) {
        ServiceCenter center = new ServiceCenter();
        center.setCenterId(dto.getCenterId());

        if (dto.getOwnerId() != null) {
            User owner = userRepository.findById(dto.getOwnerId())
                    .orElseThrow(() -> new RuntimeException("Owner not found with id: " + dto.getOwnerId()));
            center.setOwner(owner);
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
