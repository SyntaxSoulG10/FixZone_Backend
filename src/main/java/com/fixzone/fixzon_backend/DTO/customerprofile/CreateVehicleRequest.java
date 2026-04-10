package com.fixzone.fixzon_backend.DTO.customerprofile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateVehicleRequest {

    @NotBlank(message = "Brand is required")
    @Size(max = 100, message = "Brand must be at most 100 characters")
    private String brand;

    @NotBlank(message = "Model is required")
    @Size(max = 100, message = "Model must be at most 100 characters")
    private String model;

    @NotBlank(message = "Plate number is required")
    @Size(max = 20, message = "Plate number must be at most 20 characters")
    private String plateNumber;
}
