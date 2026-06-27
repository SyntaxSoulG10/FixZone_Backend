package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.ServiceCenterDTO;
import com.fixzone.fixzon_backend.DTO.ServicePackageDTO;
import com.fixzone.fixzon_backend.model.Manager;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.model.User;
import com.fixzone.fixzon_backend.repository.*;
import com.fixzone.fixzon_backend.config.AppConstants;
import com.fixzone.fixzon_backend.model.SuperAdmin;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import java.util.UUID;
import java.util.stream.Collectors;

import com.fixzone.fixzon_backend.model.ServicePackage;

/**
 * SERVICE LAYER: ServiceCenterService
 * This service manages the operational lifecycle of service center branches.
 * It handles resource mapping, owner association, and calculates real-time
 * metrics like revenue and capacity for the dashboard.
 */
@Service
@SuppressWarnings("null")
public class ServiceCenterService {
    private final ServiceCenterRepository serviceCenterRepository;
    private final UserRepository userRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final OwnerRepository ownerRepository;
    private final InvoiceRepository invoiceRepository;
    private final ManagerRepository managerRepository;
    private final SuperAdminRepository superAdminRepository;
    private final NotificationService notificationService;

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
            ManagerRepository managerRepository,
            SuperAdminRepository superAdminRepository,
            NotificationService notificationService) {
        this.serviceCenterRepository = serviceCenterRepository;
        this.userRepository = userRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.ownerRepository = ownerRepository;
        this.invoiceRepository = invoiceRepository;
        this.managerRepository = managerRepository;
        this.superAdminRepository = superAdminRepository;
        this.notificationService = notificationService;
    }

    /**
     * RETRIEVAL: Fetches all active service centers whose owner has an active subscription.
     * Inactive/expired owners' branches are hidden from customers.
     */
    public List<ServiceCenterDTO> getAllServiceCenters() {
        List<ServiceCenter> centers = serviceCenterRepository.findByIsActive(true).stream()
                .filter(center -> {
                    if (center.getOwner() == null) return true; // no owner linked — show it
                    return ownerRepository.findById(center.getOwner().getUserId())
                            .map(owner -> {
                                String status = owner.getSubscriptionStatus();
                                // Only hide if explicitly INACTIVE or EXPIRED
                                return !"INACTIVE".equals(status) && !"EXPIRED".equals(status);
                            })
                            .orElse(true); // owner not found in owner table — show it
                })
                .collect(java.util.stream.Collectors.toList());
        return mapEntitiesToDtos(centers);
    }

    public ServiceCenterDTO getServiceCenterById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Service Center ID cannot be null");
        }
        return serviceCenterRepository.findById(id)
                .map(this::mapEntityToDto)
                .orElseThrow(() -> new RuntimeException("Service center not found with id: " + id));
    }

    /**
     * SCOPED RETRIEVAL: Returns centers belonging to a specific company owner.
     * Uses optimized bulk mapping for high performance.
     */
    public List<ServiceCenterDTO> getServiceCentersByOwnerCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner code cannot be null or empty");
        }
        return ownerRepository.findByOwnerCode(code)
                .map(owner -> {
                    List<ServiceCenter> centers = serviceCenterRepository.findByOwner_UserId(owner.getUserId());
                    return mapEntitiesToDtos(centers);
                })
                .orElse(List.of());
    }

    /**
     * BULK MAPPING: Processes a list of entities in a single pass.
     * This avoids multiple database trips per entity (N+1 problem).
     */
    private List<ServiceCenterDTO> mapEntitiesToDtos(List<ServiceCenter> centers) {
        if (centers.isEmpty())
            return List.of();

        List<UUID> centerIds = centers.stream().map(ServiceCenter::getCenterId).collect(Collectors.toList());

        // BULK FETCH: Get all related data at once
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

            // Map packages
            List<ServicePackageDTO> packageDtos = packagesMap.getOrDefault(center.getCenterId(), List.of()).stream()
                    .map(pkg -> {
                        ServicePackageDTO pkgDto = new ServicePackageDTO();
                        BeanUtils.copyProperties(pkg, pkgDto);
                        pkgDto.setCenterId(center.getCenterId());
                        return pkgDto;
                    }).collect(Collectors.toList());
            dto.setServicePackages(packageDtos);

            // Map revenue
            dto.setRevenue(revenueMap.getOrDefault(center.getCenterId(), BigDecimal.ZERO));

            // Map manager
            List<Manager> managers = managersMap.getOrDefault(center.getCenterId(), List.of());
            if (!managers.isEmpty()) {
                dto.setManagerName(managers.get(0).getFullName());
            }

            // Capacity Estimation
            dto.setMechanicsCount(AppConstants.BASE_MECHANICS_COUNT + (center.getName().length() % AppConstants.MECHANICS_VARIANCE_MODULO));
            dto.setCurrentCapacity(AppConstants.BASE_CAPACITY + (center.getName().length() % AppConstants.CAPACITY_VARIANCE_MODULO));

            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * PERSISTENCE: Creates a new service center branch.
     * Automatically assigns a unique UUID if none is provided.
     */
    public ServiceCenterDTO createServiceCenter(ServiceCenterDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Service Center data cannot be null");
        }
        ServiceCenter center = mapDtoToEntity(dto);
        if (center.getCenterId() == null) {
            center.setCenterId(UUID.randomUUID());
        }
        
        if (center.getStatus() == null) {
            center.setStatus("PENDING");
        }

        ServiceCenter savedCenter = serviceCenterRepository.save(center);

        // Notify all Super Admins
        try {
            String ownerName = savedCenter.getOwner() != null ? savedCenter.getOwner().getFullName() : "An Owner";
            String message = "New registration: '" + savedCenter.getName() + "' has been registered by " + ownerName + " and is pending review.";
            List<SuperAdmin> admins = superAdminRepository.findAll();
            notificationService.broadcastNotificationSafe(admins, "New Service Center Registered", message, "INFO", "/dashboard/super-admin");
        } catch (Exception e) {
            System.err.println("Failed to trigger service center registration notification: " + e.getMessage());
        }

        return mapEntityToDto(savedCenter);
    }

    /**
     * UPDATE LOGIC: Modifies an existing service center.
     * Uses explicit field setting over generic copy to maintain fine-grained
     * control over which data is allowed to change.
     */
    public ServiceCenterDTO updateServiceCenter(UUID id, ServiceCenterDTO dto) {
        if (id == null) {
            throw new IllegalArgumentException("Target ID for update cannot be null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Service Center data cannot be null");
        }

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
        if (id == null) {
            throw new IllegalArgumentException("ID for deletion cannot be null");
        }
        
        if (!serviceCenterRepository.existsById(id)) {
            throw new IllegalStateException("Service center not found with id: " + id);
        }
        // CASCADING CLEANUP: Remove or nullify all associations before deleting the center
        // 1. Delete associated service packages
        List<ServicePackage> packages = servicePackageRepository.findByServiceCenter_CenterId(id);
        servicePackageRepository.deleteAll(packages);
        
        // 2. Clear managers' center association (or delete them if they only belong to this center)
        List<Manager> managers = managerRepository.findByManagedCenterId(id);
        for (Manager manager : managers) {
            manager.setManagedCenterId(null);
            managerRepository.save(manager);
        }
        
        // 3. Delete invoices and payment records linked to this center
        invoiceRepository.deleteAll(invoiceRepository.findByCenterId(id));
        
        // 4. Finally delete the center itself
        serviceCenterRepository.deleteById(id);
    }

    /**
     * MAPPING LOGIC (Entity to DTO):
     * This complex mapping enriches the center data with dynamic metrics
     * (revenue, managers, and service packages) for a rich UI experience.
     */
    private ServiceCenterDTO mapEntityToDto(ServiceCenter center) {
        if (center == null)
            return null;

        ServiceCenterDTO dto = new ServiceCenterDTO();
        BeanUtils.copyProperties(center, dto);

        if (center.getOwner() != null) {
            dto.setOwnerId(center.getOwner().getUserId());
            ownerRepository.findById(center.getOwner().getUserId()).ifPresent(owner -> {
                boolean stripeConnected = Boolean.TRUE.equals(owner.getStripeOnboardingComplete())
                        && owner.getStripeAccountId() != null
                        && !owner.getStripeAccountId().isBlank();
                dto.setStripeConnected(stripeConnected);
                dto.setStripeConnectionMessage(stripeConnected
                        ? "Stripe Connect is completed for this branch owner."
                        : "Stripe Connect is not completed yet. Connect Stripe to receive customer payments.");
            });
        }

        if (dto.getStripeConnected() == null) {
            dto.setStripeConnected(false);
            dto.setStripeConnectionMessage("Stripe Connect is not completed yet. Connect Stripe to receive customer payments.");
        }

        // AGGREGATION: Pull related service packages and enrich their metadata
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

        // METRICS: Calculate real revenue from issued invoices
        BigDecimal revenue = invoiceRepository.sumTotalByCenterId(center.getCenterId());
        dto.setRevenue(revenue != null ? revenue : BigDecimal.ZERO);

        // MANAGER ASSOCIATION: Identify the lead manager for this branch
        List<Manager> managers = managerRepository.findByManagedCenterId(center.getCenterId());
        if (!managers.isEmpty()) {
            dto.setManagerName(managers.get(0).getFullName());
        }

        // CAPACITY ESTIMATION: These provide realistic placeholders for operational
        // load metrics
        dto.setMechanicsCount(AppConstants.BASE_MECHANICS_COUNT + (center.getName().length() % AppConstants.MECHANICS_VARIANCE_MODULO));
        dto.setCurrentCapacity(AppConstants.BASE_CAPACITY + (center.getName().length() % AppConstants.CAPACITY_VARIANCE_MODULO));

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
