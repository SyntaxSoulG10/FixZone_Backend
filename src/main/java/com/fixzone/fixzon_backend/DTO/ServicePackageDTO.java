package com.fixzone.fixzon_backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServicePackageDTO {
    private UUID packageId;
    private String serviceName;
    private Double price;
    private String description;
    private String stationName;
}

