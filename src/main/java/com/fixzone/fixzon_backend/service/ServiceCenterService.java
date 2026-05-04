package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.ServiceCenterDTO;
import com.fixzone.fixzon_backend.DTO.ServicePackageDTO;
import com.fixzone.fixzon_backend.model.Manager;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.model.User;
import com.fixzone.fixzon_backend.repository.*;
import com.fixzone.fixzon_backend.config.AppConstants;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import java.util.UUID;
import java.util.stream.Collectors;

import com.fixzone.fixzon_backend.model.ServicePackage;


@Service
public class ServiceCenterService {

    
    private final ServiceCenterRepository serviceCenterRepository;
    
    
    private final UserRepository userRepository;
    
    
    private final ServicePackageRepository servicePackageRepository;
    
    
    private final OwnerRepository ownerRepository;
    
    
    private final InvoiceRepository invoiceRepository;
    
   
    private final ManagerRepository managerRepository;

    /**
     * Constructor-based dependency injection
     * Ensures all required repositories are provided at startup to prevent NPE at runtime
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

    // Retrieves all active service centers with enriched data (revenue, managers, packages)
    public List<ServiceCenterDTO> getAllServiceCenters() {
        try {
            List<ServiceCenter> centers = serviceCenterRepository.findByIsActive(true);
            return mapEntitiesToDtos(centers);
        } catch (Exception e) {
            System.err.println("Database error while retrieving service centers: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve service centers", e);
        }
    }

    // Retrieves a service center by ID with full details
    public ServiceCenterDTO getServiceCenterById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Service Center ID cannot be null");
        }
        try {
            return serviceCenterRepository.findById(id)
                    .map(this::mapEntityToDto)
                    .orElseThrow(() -> new RuntimeException("Service center not found with id: " + id));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Database error while retrieving service center: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve service center", e);
        }
    }

    // Retrieves all service centers owned by a specific company
    public List<ServiceCenterDTO> getServiceCentersByOwnerCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner code cannot be null or empty");
        }
        try {
            return ownerRepository.findByOwnerCode(code)
                    .map(owner -> {
                        List<ServiceCenter> centers = serviceCenterRepository.findByOwner_UserId(owner.getUserId());
                        return mapEntitiesToDtos(centers);
                    })
                    .orElse(List.of());
        } catch (Exception e) {
            System.err.println("Database error while retrieving service centers by owner code: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve service centers by owner code", e);
        }
    }

    // Bulk mapping with optimized queries to avoid N+1 problem
    private List<ServiceCenterDTO> mapEntitiesToDtos(List<ServiceCenter> centers) {
        if (centers.isEmpty())
            return List.of();

        List<UUID> centerIds = centers.stream().map(ServiceCenter::getCenterId).collect(Collectors.toList());

        Map<UUID, BigDecimal> revenueMap = invoiceRepository.sumTotalByCenterIdIn(centerIds).stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (BigDecimal) row[1], (a, b) -> a));

        Map<UUID, List<ServicePackage>> packagesMap = servicePackageRepository
                .findByServiceCenter_CenterIdInAndIsActiveTrue(centerIds).stream()
                .collect(Collectors.groupingBy(pkg -> pkg.getServiceCenter().getCenterId()));

        Map<UUID, List<Manager>> managersMap = managerRepository.findByManagedCenterIdIn(centerIds).stream()
                .collect(Collectors.groupingBy(Manager::getManagedCenterId));

        return centers.stream().map(center -> {
            ServiceCenterDTO dto = new ServiceCenterDTO();
            BeanUtils.copyProperties(center, dto);

            if (center.getOwner() != null) {
                dto.setOwnerId(center.getOwner().getUserId());
            }

            List<ServicePackageDTO> packageDtos = packagesMap.getOrDefault(center.getCenterId(), List.of()).stream()
                    .map(pkg -> {
                        ServicePackageDTO pkgDto = new ServicePackageDTO();
                        BeanUtils.copyProperties(pkg, pkgDto);
                        pkgDto.setCenterId(center.getCenterId());
                        return pkgDto;
                    }).collect(Collectors.toList());
            dto.setServicePackages(packageDtos);

            dto.setRevenue(revenueMap.getOrDefault(center.getCenterId(), BigDecimal.ZERO));

            List<Manager> managers = managersMap.getOrDefault(center.getCenterId(), List.of());
            if (!managers.isEmpty()) {
                dto.setManagerName(managers.get(0).getFullName());
            }

            dto.setMechanicsCount(AppConstants.BASE_MECHANICS_COUNT + (center.getName().length() % AppConstants.MECHANICS_VARIANCE_MODULO));
            dto.setCurrentCapacity(AppConstants.BASE_CAPACITY + (center.getName().length() % AppConstants.CAPACITY_VARIANCE_MODULO));

            return dto;
        }).collect(Collectors.toList());
    }

    // Creates a new service center (auto-generates UUID if needed)
    public ServiceCenterDTO createServiceCenter(ServiceCenterDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Service Center data cannot be null");
        }
        try {
            ServiceCenter center = mapDtoToEntity(dto);
            if (center.getCenterId() == null) {
                center.setCenterId(UUID.randomUUID());
            }
            return mapEntityToDto(serviceCenterRepository.save(center));
        } catch (Exception e) {
            System.err.println("Database error while creating service center: " + e.getMessage());
            throw new RuntimeException("Failed to create service center", e);
        }
    }

    // Updates an existing service center
    public ServiceCenterDTO updateServiceCenter(UUID id, ServiceCenterDTO dto) {
        if (id == null) {
            throw new IllegalArgumentException("Target ID for update cannot be null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Service Center data cannot be null");
        }

        try {
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
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Database error while updating service center: " + e.getMessage());
            throw new RuntimeException("Failed to update service center", e);
        }
    }

    // Deletes a service center with cascading cleanup
    public void deleteServiceCenter(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID for deletion cannot be null");
        }
        
        try {
            if (!serviceCenterRepository.existsById(id)) {
                throw new IllegalStateException("Service center not found with id: " + id);
            }
            
            // Delete associated packages, clear manager associations, delete invoices
            List<ServicePackage> packages = servicePackageRepository.findByServiceCenter_CenterId(id);
            servicePackageRepository.deleteAll(packages);
            
            List<Manager> managers = managerRepository.findByManagedCenterId(id);
            for (Manager manager : managers) {
                manager.setManagedCenterId(null);
                managerRepository.save(manager);
            }
            
            invoiceRepository.deleteAll(invoiceRepository.findByCenterId(id));
            serviceCenterRepository.deleteById(id);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Database error while deleting service center: " + e.getMessage());
            throw new RuntimeException("Failed to delete service center", e);
        }
    }

    // Converts entity to DTO with enriched data (packages, revenue, manager)
    private ServiceCenterDTO mapEntityToDto(ServiceCenter center) {
        if (center == null)
            return null;

        ServiceCenterDTO dto = new ServiceCenterDTO();
        BeanUtils.copyProperties(center, dto);

        if (center.getOwner() != null) {
            dto.setOwnerId(center.getOwner().getUserId());
        }

        List<ServicePackageDTO> packages = servicePackageRepository
                .findByServiceCenter_CenterIdAndIsActiveTrue(center.getCenterId())
                .stream()
                .map(pkg -> {
                    ServicePackageDTO pkgDto = new ServicePackageDTO();
                    BeanUtils.copyProperties(pkg, pkgDto);
                    pkgDto.setCenterId(center.getCenterId());
                    return pkgDto;
                })
                .collect(Collectors.toList());
        dto.setServicePackages(packages);

        // Step 4: Calculate real revenue from issued invoices at this center
        BigDecimal revenue = invoiceRepository.sumTotalByCenterId(center.getCenterId());
        dto.setRevenue(revenue != null ? revenue : BigDecimal.ZERO);

        // Step 5: Identify and map the lead manager for this branch
        List<Manager> managers = managerRepository.findByManagedCenterId(center.getCenterId());
        if (!managers.isEmpty()) {
            dto.setManagerName(managers.get(0).getFullName());
        }

        dto.setMechanicsCount(AppConstants.BASE_MECHANICS_COUNT + (center.getName().length() % AppConstants.MECHANICS_VARIANCE_MODULO));
        dto.setCurrentCapacity(AppConstants.BASE_CAPACITY + (center.getName().length() % AppConstants.CAPACITY_VARIANCE_MODULO));

        return dto;
    }

    // Converts DTO to entity with owner relationship mapping
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
