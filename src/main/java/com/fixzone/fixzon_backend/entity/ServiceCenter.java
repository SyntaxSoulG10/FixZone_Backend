package com.fixzone.fixzon_backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "service_centers")
public class ServiceCenter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String streetName;
    private String city;
    private String district;
    private String postalCode;

    private String status; // PENDING, APPROVED, SUSPENDED
    private String type;
    private String brNumber;
    private String brDocumentUrl;
    
    @Column(precision = 2, scale = 1)
    private java.math.BigDecimal reviewScore;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    public ServiceCenter() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStreetName() { return streetName; }
    public void setStreetName(String streetName) { this.streetName = streetName; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getBrNumber() { return brNumber; }
    public void setBrNumber(String brNumber) { this.brNumber = brNumber; }
    public String getBrDocumentUrl() { return brDocumentUrl; }
    public void setBrDocumentUrl(String brDocumentUrl) { this.brDocumentUrl = brDocumentUrl; }
    public java.math.BigDecimal getReviewScore() { return reviewScore; }
    public void setReviewScore(java.math.BigDecimal reviewScore) { this.reviewScore = reviewScore; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
}
