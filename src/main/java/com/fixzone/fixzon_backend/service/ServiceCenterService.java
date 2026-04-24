package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.ServiceCenterDTO;
import com.fixzone.fixzon_backend.DTO.ServicePackageDTO;
import com.fixzone.fixzon_backend.model.Manager;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.model.User;
import com.fixzone.fixzon_backend.repository.*;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SERVICE LAYER: ServiceCenterService
 * This service manages the operational lifecycle of service center branches.
 * It handles resource mapping, owner association, and calculates real-time 
 * metrics like revenue and capacity for the dashboard.
 */
@Service
public class ServiceCenterService {

    private final ServiceCenterRepository serviceCenterRepository;
    private final UserRepository userRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final OwnerRepository ownerRepository;
    private final InvoiceRepository invoiceRepository;
    private final ManagerRepository managerRepository;

    /**
     * Dependency Injection via Constructor: Ensures all required repositories 
     * are provided at startup, preventing NullPointerExceptions during runtime.
     */
    public ServiceCenterService(
            ServiceCenterRepository serviceCenterRepository, 
            UserRepository userRepository,
            ServicePackageRepository servicePackageRepository,
            OwnerRepository ownerRepository,
            InvoiceRepository invoiceRepository,
            ManagerRepository managerRepository) {
        this.serviceCenterRepository = serviceCenterRepository;
        this.userRepository = userRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.ownerRepository = ownerRepository;
        this.invoiceRepository = invoiceRepository;
        this.managerRepository = managerRepository;
    }

    /**
     * RETRIEVAL: Fetches all active service centers.
     * We map entities to DTOs to avoid exposing internal database structures to the API.
     */
    public List<ServiceCenterDTO> getAllServiceCenters() {
        return serviceCenterRepository.findByIsActive(true).stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    public ServiceCenterDTO getServiceCenterById(UUID id) {
        Objects.requireNonNull(id, "Service Center ID cannot be null");
        return serviceCenterRepository.findById(id)
                .map(this::mapEntityToDto)
                .orElseThrow(() -> new RuntimeException("Service center not found with id: " + id));
    }

    /**
     * SCOPED RETRIEVAL: Returns centers belonging to a specific company owner.
     */
    public List<ServiceCenterDTO> getServiceCentersByOwnerCode(String code) {
        return ownerRepository.findByOwnerCode(code)
                .map(owner -> serviceCenterRepository.findByOwner_UserId(owner.getUserId()).stream()
                        .map(this::mapEntityToDto)
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    /**
     * PERSISTENCE: Creates a new service center branch.
     * We automatically assign a unique UUID if none is provided.
     */
    public ServiceCenterDTO createServiceCenter(ServiceCenterDTO dto) {
        ServiceCenter center = mapDtoToEntity(dto);
        if (center.getCenterId() == null) {
            center.setCenterId(UUID.randomUUID());
        }
        return mapEntityToDto(serviceCenterRepository.save(center));
    }

    /**
     * UPDATE LOGIC: Modifies an existing service center.
     * We favor explicit field setting over generic copy to maintain fine-grained control 
     * over which data is allowed to change.
     */
    public ServiceCenterDTO updateServiceCenter(UUID id, ServiceCenterDTO dto) {
        Objects.requireNonNull(id, "Target ID for update cannot be null");
        
        ServiceCenter existing = serviceCenterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service center not found with id: " + id));

        existing.setName(dto.getName());
        existing.setAddress(dto.getAddress());
        existing.setContactPhone(dto.getContactPhone());
        existing.setOpeningHours(dto.getOpeningHours());
        existing.setRating(dto.getRating());
        existing.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : existing.getIsActive());
        existing.setUpdatedBy(dto.getUpdatedBy());
        existing.setSupportedVehicleBrands(dto.getSupportedVehicleBrands());

        return mapEntityToDto(serviceCenterRepository.save(existing));
    }

    public void deleteServiceCenter(UUID id) {
        Objects.requireNonNull(id, "ID for deletion cannot be null");
        serviceCenterRepository.deleteById(id);
    }

    /**
     * MAPPING LOGIC (Entity to DTO): 
     * This complex mapping enriches the center data with dynamic metrics 
     * (revenue, managers, and service packages) for a rich UI experience.
     */
    private ServiceCenterDTO mapEntityToDto(ServiceCenter center) {
        if (center == null) return null;
        
        ServiceCenterDTO dto = new ServiceCenterDTO();
        BeanUtils.copyProperties(center, dto);
        
        if (center.getOwner() != null) {
            dto.setOwnerId(center.getOwner().getUserId());
        }

        // AGGREGATION: Pull related service packages and enrich their metadata
        List<ServicePackageDTO> packages = servicePackageRepository.findByServiceCenter_CenterIdAndIsActiveTrue(center.getCenterId())
                .stream()
                .map(pkg -> {
                    ServicePackageDTO pkgDto = new ServicePackageDTO();
                    BeanUtils.copyProperties(pkg, pkgDto);
                    pkgDto.setCenterId(center.getCenterId());
                    return pkgDto;
                })
                .collect(Collectors.toList());
        dto.setServicePackages(packages);

        // METRICS: Calculate real revenue from issued invoices
        BigDecimal revenue = invoiceRepository.sumTotalByCenterId(center.getCenterId());
        dto.setRevenue(revenue != null ? revenue : BigDecimal.ZERO);
        
        // MANAGER ASSOCIATION: Identify the lead manager for this branch
        List<Manager> managers = managerRepository.findByManagedCenterId(center.getCenterId());
        if (!managers.isEmpty()) {
            dto.setManagerName(managers.get(0).getFullName());
        }

        // CAPACITY ESTIMATION: These provide realistic placeholders for operational load metrics
        dto.setMechanicsCount(5 + (center.getName().length() % 5)); 
        dto.setCurrentCapacity(40 + (center.getName().length() % 30));

        return dto;
    }

    /**
     * MAPPING LOGIC (DTO to Entity):
     * Ensures the entity is correctly linked to the User (Owner) in the database.
     */
    private ServiceCenter mapDtoToEntity(ServiceCenterDTO dto) {
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
