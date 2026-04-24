package com.fixzone.fixzon_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "owner")
@PrimaryKeyJoinColumn(name = "user_id")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Owner extends User {

    @Column(name = "owner_code", unique = true, nullable = false, length = 50)
    private String ownerCode;

    @Column(name = "company_name", length = 150)
    private String companyName;

    @Column(name = "company_email", length = 150)
    private String companyEmail;

    @Column(name = "company_number", length = 20)
    private String companyNumber;

    @Column(name = "banner_image_url", columnDefinition = "TEXT")
    private String bannerImageUrl;

    public Owner(UUID userId, String fullName, String email, String phone, String passwordHash, String role,
            Boolean emailVerified, LocalDateTime lastLoginAt, LocalDateTime createdAt, String createdBy,
            LocalDateTime updatedAt, String updatedBy, String profilePictureUrl, String ownerCode, String companyName, String companyEmail,
            String companyNumber, String bannerImageUrl) {
        super(userId, fullName, email, phone, passwordHash, role, emailVerified, lastLoginAt, createdAt, createdBy,
                updatedAt, updatedBy, "Active", profilePictureUrl);
        this.ownerCode = ownerCode;
        this.companyName = companyName;
        this.companyEmail = companyEmail;
        this.companyNumber = companyNumber;
        this.bannerImageUrl = bannerImageUrl;
    }

}
