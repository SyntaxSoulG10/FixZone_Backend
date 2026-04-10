package com.fixzone.fixzon_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "customer_vehicles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Customer ID is required")
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @NotBlank(message = "Brand is required")
    @Column(nullable = false, length = 100)
    private String brand;

    @NotBlank(message = "Model is required")
    @Column(nullable = false, length = 100)
    private String model;

    @NotBlank(message = "Plate number is required")
    @Column(name = "plate_number", nullable = false, unique = true, length = 20)
    private String plateNumber;
}
