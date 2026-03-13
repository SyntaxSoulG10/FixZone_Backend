package com.fixzone.fixzon_backend.entity;

import com.fixzone.fixzon_backend.enums.Role;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String status; // Active, Suspended, Pending
    private LocalDateTime createdAt;

    // Common fields
    private String contactNo;
    private String addressStreet;
    private String addressCity;
    private String addressDistrict;
    private String addressPostalCode;
    
    // Customer specific
    private String profileImageUrl;
    private String authMethod;
    private String mobileOtp;
    private String notificationPreference; // Email, SMS, Both

    public User() {}

    public User(String name, String email, String password, Role role, String status, 
                String contactNo, String addressStreet, String addressCity, 
                String addressDistrict, String addressPostalCode) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = status;
        this.contactNo = contactNo;
        this.addressStreet = addressStreet;
        this.addressCity = addressCity;
        this.addressDistrict = addressDistrict;
        this.addressPostalCode = addressPostalCode;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "Active";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public String getContactNo() { return contactNo; }
    public void setContactNo(String contactNo) { this.contactNo = contactNo; }
    public String getAddressStreet() { return addressStreet; }
    public void setAddressStreet(String addressStreet) { this.addressStreet = addressStreet; }
    public String getAddressCity() { return addressCity; }
    public void setAddressCity(String addressCity) { this.addressCity = addressCity; }
    public String getAddressDistrict() { return addressDistrict; }
    public void setAddressDistrict(String addressDistrict) { this.addressDistrict = addressDistrict; }
    public String getAddressPostalCode() { return addressPostalCode; }
    public void setAddressPostalCode(String addressPostalCode) { this.addressPostalCode = addressPostalCode; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public String getAuthMethod() { return authMethod; }
    public void setAuthMethod(String authMethod) { this.authMethod = authMethod; }
    public String getMobileOtp() { return mobileOtp; }
    public void setMobileOtp(String mobileOtp) { this.mobileOtp = mobileOtp; }
    public String getNotificationPreference() { return notificationPreference; }
    public void setNotificationPreference(String notificationPreference) { this.notificationPreference = notificationPreference; }

    // Builder pattern equivalent
    public static UserBuilder builder() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private String name;
        private String email;
        private String password;
        private Role role;
        private String status;
        private String contactNo;
        private String addressStreet;
        private String addressCity;
        private String addressDistrict;
        private String addressPostalCode;

        public UserBuilder name(String name) { this.name = name; return this; }
        public UserBuilder email(String email) { this.email = email; return this; }
        public UserBuilder password(String password) { this.password = password; return this; }
        public UserBuilder role(Role role) { this.role = role; return this; }
        public UserBuilder status(String status) { this.status = status; return this; }
        public UserBuilder contactNo(String contactNo) { this.contactNo = contactNo; return this; }
        public UserBuilder addressStreet(String addressStreet) { this.addressStreet = addressStreet; return this; }
        public UserBuilder addressCity(String addressCity) { this.addressCity = addressCity; return this; }
        public UserBuilder addressDistrict(String addressDistrict) { this.addressDistrict = addressDistrict; return this; }
        public UserBuilder addressPostalCode(String addressPostalCode) { this.addressPostalCode = addressPostalCode; return this; }

        public User build() {
            return new User(name, email, password, role, status, contactNo, addressStreet, addressCity, addressDistrict, addressPostalCode);
        }
    }
}
