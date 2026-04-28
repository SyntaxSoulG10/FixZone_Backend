package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.VehicleDTO;
import com.fixzone.fixzon_backend.model.Vehicle;
import com.fixzone.fixzon_backend.repository.VehicleRepository;
import com.fixzone.fixzon_backend.repository.BookingRepository;
import com.fixzone.fixzon_backend.enums.BookingStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VehicleService {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private BookingRepository bookingRepository;

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
        // Define statuses that block editing
        Collection<BookingStatus> activeStatuses = Arrays.asList(
                BookingStatus.PENDING,
                BookingStatus.PENDING_PAYMENT,
                BookingStatus.CONFIRMED,
                BookingStatus.IN_PROGRESS
        );

        // Check if any active bookings exist for this vehicle
        boolean hasActiveBookings = bookingRepository.existsByVehicleIdAndStatusIn(vehicleId, activeStatuses);

        if (hasActiveBookings) {
            throw new RuntimeException("Cannot edit vehicle details with active or pending bookings. Please complete or cancel your bookings first.");
        }

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
        // Define statuses that block deletion
        Collection<BookingStatus> activeStatuses = Arrays.asList(
                BookingStatus.PENDING,
                BookingStatus.PENDING_PAYMENT,
                BookingStatus.CONFIRMED,
                BookingStatus.IN_PROGRESS
        );

        // Check if any active bookings exist for this vehicle
        boolean hasActiveBookings = bookingRepository.existsByVehicleIdAndStatusIn(vehicleId, activeStatuses);

        if (hasActiveBookings) {
            throw new RuntimeException("Cannot delete vehicle with active or pending bookings. Please complete or cancel your bookings first.");
        }

        vehicleRepository.deleteById(vehicleId);
    }

    private VehicleDTO convertToDTO(Vehicle vehicle) {
        Integer daysSinceService = null;
        if (vehicle.getLastServiceDate() != null && !vehicle.getLastServiceDate().isEmpty()) {
            try {
                // Expecting format DD/MM/YYYY
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                LocalDate serviceDate = LocalDate.parse(vehicle.getLastServiceDate(), formatter);
                LocalDate today = LocalDate.now();
                daysSinceService = (int) ChronoUnit.DAYS.between(serviceDate, today);
            } catch (Exception e) {
                // Fallback or ignore if format is invalid
            }
        }

        return new VehicleDTO(
                vehicle.getId(),
                vehicle.getCustomerId(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getVehicleType(),
                vehicle.getPlateNumber(),
                vehicle.getImageUrl(),
                vehicle.getLastServiceDate(),
                daysSinceService
        );
    }
}
