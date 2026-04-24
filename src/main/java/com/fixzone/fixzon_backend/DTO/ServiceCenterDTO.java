package com.fixzone.fixzon_backend.DTO;

import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "Service center name is required")
    @jakarta.validation.constraints.Size(min = 3, max = 100, message = "Name must be 3-100 characters")
    private String name;

    @NotBlank(message = "Manager name is required")
    private String managerName;

    @NotBlank(message = "Address is required")
    @jakarta.validation.constraints.Size(min = 5, message = "Address is too short")
    private String address;

    @NotBlank(message = "Contact phone is required")
    @jakarta.validation.constraints.Pattern(regexp = "^[0-9+]{10,15}$", message = "Phone must be 10-15 digits")
    private String contactPhone;

    private String openingHours;
    private BigDecimal rating;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private String[] supportedVehicleBrands;
    private String status;
    private String businessRegUrl;
    private String taxIdUrl;
    private String nicUrl;
    private String rejectionReason;
    private java.util.List<ServicePackageDTO> servicePackages;
    private BigDecimal revenue;
    private Integer mechanicsCount;
    private Integer currentCapacity;
}
