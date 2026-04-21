package com.fixzone.fixzon_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "service_center_manager")
@PrimaryKeyJoinColumn(name = "user_id")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Manager extends User {

    @Column(name = "manager_code", unique = true, nullable = false, length = 50)
    private String managerCode;

    @Column(name = "managed_center_id")
    private UUID managedCenterId;

    public Manager(UUID userId, String fullName, String email, String phone, String passwordHash, String role,
            Boolean emailVerified, LocalDateTime lastLoginAt, LocalDateTime createdAt, String createdBy,
            LocalDateTime updatedAt, String updatedBy, String profilePictureUrl, String managerCode, UUID managedCenterId) {
        super(userId, fullName, email, phone, passwordHash, role, emailVerified, lastLoginAt, createdAt, createdBy,
                updatedAt, updatedBy, "Active", profilePictureUrl);
        this.managerCode = managerCode;
        this.managedCenterId = managedCenterId;
    }

}
