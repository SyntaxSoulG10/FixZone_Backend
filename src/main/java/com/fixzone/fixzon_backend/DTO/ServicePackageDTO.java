package com.fixzone.fixzon_backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServicePackageDTO {
    private UUID packageId;
    private UUID centerId;
    private String name;
    private String type;
    private String description;
    private BigDecimal basePrice;
    private Integer estimatedDurationMins;
    private Boolean isActive;
}
