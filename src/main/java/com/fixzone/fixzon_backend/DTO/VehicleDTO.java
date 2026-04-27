package com.fixzone.fixzon_backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDTO {
    private UUID id;
    private UUID customerId;
    private String brand;
    private String model;
    private String vehicleType;
    private String plateNumber;
    private String imageUrl;
    private String lastServiceDate;
    private Integer daysSinceService;
}
