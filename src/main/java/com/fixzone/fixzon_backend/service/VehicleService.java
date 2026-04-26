package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.VehicleDTO;
import com.fixzone.fixzon_backend.model.Vehicle;
import com.fixzone.fixzon_backend.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VehicleService {

    @Autowired
    private VehicleRepository vehicleRepository;

    public List<VehicleDTO> getVehiclesByCustomerId(UUID customerId) {
        return vehicleRepository.findByCustomerId(customerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public VehicleDTO createVehicle(VehicleDTO dto) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(UUID.randomUUID());
        vehicle.setCustomerId(dto.getCustomerId());
        vehicle.setBrand(dto.getBrand());
        vehicle.setModel(dto.getModel());
        vehicle.setVehicleType(dto.getVehicleType());
        vehicle.setPlateNumber(dto.getPlateNumber());
        vehicle.setImageUrl(dto.getImageUrl());
        vehicle.setLastServiceDate(dto.getLastServiceDate());
        return convertToDTO(vehicleRepository.save(vehicle));
    }

    public VehicleDTO updateVehicle(UUID vehicleId, VehicleDTO dto) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        if (dto.getBrand() != null) vehicle.setBrand(dto.getBrand());
        if (dto.getModel() != null) vehicle.setModel(dto.getModel());
        if (dto.getVehicleType() != null) vehicle.setVehicleType(dto.getVehicleType());
        if (dto.getPlateNumber() != null) vehicle.setPlateNumber(dto.getPlateNumber());
        if (dto.getImageUrl() != null) vehicle.setImageUrl(dto.getImageUrl());
        if (dto.getLastServiceDate() != null) vehicle.setLastServiceDate(dto.getLastServiceDate());
        return convertToDTO(vehicleRepository.save(vehicle));
    }

    public void deleteVehicle(UUID vehicleId) {
        vehicleRepository.deleteById(vehicleId);
    }

    private VehicleDTO convertToDTO(Vehicle vehicle) {
        return new VehicleDTO(
                vehicle.getId(),
                vehicle.getCustomerId(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getVehicleType(),
                vehicle.getPlateNumber(),
                vehicle.getImageUrl(),
                vehicle.getLastServiceDate()
        );
    }
}
