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
        return repository.findByIsActiveTrue().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServicePackageDTO> getPackagesByCenter(UUID centerId) {
        if (centerId == null) {
            throw new IllegalArgumentException("Center ID cannot be null");
        }
        return repository.findByServiceCenter_CenterIdAndIsActiveTrue(centerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Returns packages for a center filtered by vehicle type.
     * Packages with no vehicleType restriction are always included.
     */
    @Transactional(readOnly = true)
    public List<ServicePackageDTO> getPackagesByCenterAndVehicleType(UUID centerId, String vehicleType) {
        if (centerId == null) throw new IllegalArgumentException("Center ID cannot be null");
        if (vehicleType == null || vehicleType.isBlank()) {
            return getPackagesByCenter(centerId);
        }
        return repository.findByCenterIdAndVehicleType(centerId, vehicleType.toUpperCase()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServicePackageDTO> getPackagesByOwnerEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        return repository.findPackagesByOwnerEmail(email).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServicePackageDTO> getPackagesByOwnerCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner code cannot be null or empty");
        }
        return repository.findPackagesByOwnerCode(code).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServicePackageDTO getPackageById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
        return repository.findById(id)
                .map(this::convertToDTO)
                .orElse(null);
    }

    public ServicePackageDTO createPackage(ServicePackageDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Service Package data cannot be null");
        }
        validateBrandAndType(dto.getVehicleType(), dto.getVehicleBrand());

        ServicePackage model = new ServicePackage();
        BeanUtils.copyProperties(dto, model, "packageId", "createdAt");
        
        if (dto.getCenterId() != null) {
            model.setServiceCenter(centerRepository.findById(dto.getCenterId())
                .orElseThrow(() -> new RuntimeException("Service Center not found with id: " + dto.getCenterId())));
        }
        
        ServicePackage saved = repository.save(model);
        return convertToDTO(saved);
    }

    public ServicePackageDTO updatePackage(UUID id, ServicePackageDTO dto) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Service Package data cannot be null");
        }
        validateBrandAndType(dto.getVehicleType(), dto.getVehicleBrand());

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
    }

    private void validateBrandAndType(String vehicleType, String vehicleBrand) {
        if (vehicleBrand == null || vehicleBrand.isBlank() || vehicleBrand.equalsIgnoreCase("ALL") || 
            vehicleType == null || vehicleType.isBlank() || vehicleType.equalsIgnoreCase("ALL")) {
            return;
        }

        String type = vehicleType.toUpperCase().trim();
        String brand = vehicleBrand.trim();

        if ("BIKE".equals(type)) {
            List<String> incompatibleForBike = List.of("Toyota", "Nissan", "Hyundai", "Kia", "Mazda", "Audi", "Mercedes-Benz", "Subaru", "Lexus", "Tata", "Mahindra", "Ford", "Land Rover");
            if (incompatibleForBike.stream().anyMatch(b -> b.equalsIgnoreCase(brand))) {
                throw new IllegalArgumentException(brand + " does not manufacture motorcycles or scooters. Please select a valid motorcycle brand (e.g., Honda, Yamaha, Suzuki, Bajaj, TVS, BMW) or change the vehicle classification.");
            }
        } else if ("BUS".equals(type)) {
            List<String> incompatibleForBus = List.of("BMW", "Audi", "Honda", "Suzuki", "Mazda", "Subaru", "Lexus", "Yamaha", "Bajaj", "TVS", "Kia", "Hyundai", "Land Rover", "Ford");
            if (incompatibleForBus.stream().anyMatch(b -> b.equalsIgnoreCase(brand))) {
                throw new IllegalArgumentException(brand + " does not manufacture commercial passenger buses. Compatible bus brands include Toyota (Coaster), Nissan (Civilian), Mercedes-Benz, Tata, Ashok Leyland, Mitsubishi, etc.");
            }
        } else if ("CAR".equals(type) || "SUV".equals(type)) {
            List<String> bikeOnly = List.of("Yamaha", "Bajaj", "TVS");
            if (bikeOnly.stream().anyMatch(b -> b.equalsIgnoreCase(brand))) {
                throw new IllegalArgumentException(brand + " is a motorcycle manufacturer and does not produce passenger cars or SUVs.");
            }
        } else if ("VAN".equals(type)) {
            List<String> incompatibleForVan = List.of("Yamaha", "Bajaj", "TVS", "Audi", "BMW", "Subaru", "Lexus", "Land Rover");
            if (incompatibleForVan.stream().anyMatch(b -> b.equalsIgnoreCase(brand))) {
                throw new IllegalArgumentException(brand + " does not manufacture commercial passenger or cargo vans.");
            }
        } else if ("TRUCK".equals(type)) {
            List<String> incompatibleForTruck = List.of("Yamaha", "Bajaj", "TVS", "Audi", "BMW", "Subaru", "Lexus");
            if (incompatibleForTruck.stream().anyMatch(b -> b.equalsIgnoreCase(brand))) {
                throw new IllegalArgumentException(brand + " does not manufacture commercial lorries or pickup trucks.");
            }
        }
    }

    public void deletePackage(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
        if (!repository.existsById(id)) {
            throw new IllegalStateException("Service package not found with id: " + id);
        }
        repository.deleteById(id);
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
