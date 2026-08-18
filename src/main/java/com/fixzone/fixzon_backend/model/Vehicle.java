package com.fixzone.fixzon_backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "vehicles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "brand", nullable = false)
    private String brand;

    @Column(name = "plate_number", nullable = false, unique = true)
    private String plateNumber;

    /**
     * Vehicle type used to filter compatible service packages.
     * Expected values: "CAR", "BIKE", "VAN", "TRUCK"
     */
    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "vehicle_type", length = 20)
    private String vehicleType;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
