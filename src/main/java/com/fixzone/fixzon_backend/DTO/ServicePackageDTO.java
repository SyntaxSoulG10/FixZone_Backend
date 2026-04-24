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
    private String name;

    private String type;
    private String description;

    @NotNull(message = "Price is required")
    private BigDecimal basePrice;

    private Integer estimatedDurationMins;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
