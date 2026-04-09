package com.fixzone.fixzon_backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCenterDTO {
    private UUID centerId;
    private UUID ownerId;
    private String name;
    private String address;
    private String contactPhone;
    private String openingHours;
    private BigDecimal rating;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private String[] supportedVehicleBrands;
    private java.util.List<ServicePackageDTO> servicePackages;
}
