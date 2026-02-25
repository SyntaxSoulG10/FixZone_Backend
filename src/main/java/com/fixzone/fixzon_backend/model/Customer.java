package com.fixzone.fixzon_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Customer extends User {

    @Column(name = "customer_code", unique = true, nullable = false, length = 50)
    private String customerCode;

    @Column(name = "preferred_contact_method", length = 50)
    private String preferredContactMethod;

    public Customer(UUID userId, String fullName, String email, String phone, String passwordHash, String role,
            Boolean emailVerified, LocalDateTime lastLoginAt, LocalDateTime createdAt, String createdBy,
            LocalDateTime updatedAt, String updatedBy, String customerCode, String preferredContactMethod) {
        super(userId, fullName, email, phone, passwordHash, role, emailVerified, lastLoginAt, createdAt, createdBy,
                updatedAt, updatedBy);
        this.customerCode = customerCode;
        this.preferredContactMethod = preferredContactMethod;
    }
}
