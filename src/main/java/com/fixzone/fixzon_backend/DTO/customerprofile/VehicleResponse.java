package com.fixzone.fixzon_backend.DTO.customerprofile;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VehicleResponse {
    private Long id;
    private String brand;
    private String model;
    private String plateNumber;
}
