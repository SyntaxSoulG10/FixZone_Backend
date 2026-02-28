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

    @Column(name = "visits")
    private Integer visits = 0;

    @Column(name = "total_spent")
    private Double totalSpent = 0.0;

    @Column(name = "status", length = 20)
    private String status = "New";

    @Column(name = "avatar_url")
    private String avatarUrl;

    public Customer(UUID userId, String fullName, String email, String phone, String passwordHash, String role,
            Boolean emailVerified, LocalDateTime lastLoginAt, LocalDateTime createdAt, String createdBy,
            LocalDateTime updatedAt, String updatedBy, String customerCode, String preferredContactMethod,
            Integer visits, Double totalSpent, String status, String avatarUrl) {
        super(userId, fullName, email, phone, passwordHash, role, emailVerified, lastLoginAt, createdAt, createdBy,
                updatedAt, updatedBy);
        this.customerCode = customerCode;
        this.preferredContactMethod = preferredContactMethod;
        this.visits = visits;
        this.totalSpent = totalSpent;
        this.status = status;
        this.avatarUrl = avatarUrl;
    }

    // Overloaded constructor for compatibility with existing code
    public Customer(UUID userId, String fullName, String email, String phone, String passwordHash, String role,
            Boolean emailVerified, LocalDateTime lastLoginAt, LocalDateTime createdAt, String createdBy,
            LocalDateTime updatedAt, String updatedBy, String customerCode, String preferredContactMethod) {
        this(userId, fullName, email, phone, passwordHash, role, emailVerified, lastLoginAt, createdAt, createdBy,
                updatedAt, updatedBy, customerCode, preferredContactMethod, 0, 0.0, "New", null);
    }
}
