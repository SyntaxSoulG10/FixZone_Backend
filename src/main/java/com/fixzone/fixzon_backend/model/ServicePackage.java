package com.fixzone.fixzon_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "service_packages")
@Data
@NoArgsConstructor
public class ServicePackage {

    @Id
    @Column(name = "package_id", nullable = false, updatable = false)
    private UUID packageId;

    @Column(name = "name", nullable = true, length = 150)
    private String serviceName;

    @Column(name = "base_price", nullable = true)
    private Double price;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "type", length = 100)
    private String type;

    @Column(name = "duration", length = 100)
    private String duration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Owner owner;
}

