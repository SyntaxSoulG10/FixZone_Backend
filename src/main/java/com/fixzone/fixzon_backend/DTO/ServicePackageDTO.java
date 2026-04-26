package com.fixzone.fixzon_backend.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServicePackageDTO {
    private UUID packageId;
    private UUID centerId;

    @NotBlank(message = "Service name is required")
    @jakarta.validation.constraints.Size(min = 3, max = 100, message = "Service name must be 3-100 characters")
    private String name;

    private String type;
    private String vehicleType;
    
    @NotBlank(message = "Description is required")
    @jakarta.validation.constraints.Size(min = 10, message = "Description must be at least 10 characters")
    private String description;

    @NotNull(message = "Price is required")
    @jakarta.validation.constraints.DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal basePrice;

    @NotNull(message = "Duration is required")
    @jakarta.validation.constraints.Min(value = 5, message = "Minimum duration is 5 minutes")
    @jakarta.validation.constraints.Max(value = 1440, message = "Maximum duration is 24 hours")
    private Integer estimatedDurationMins;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
