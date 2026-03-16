package com.fixzone.fixzon_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "analytics")
public class Analytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String metrics; // JSON or comma-separated values describing metrics
    private LocalDate date;

    @ManyToOne
    @JoinColumn(name = "service_center_id")
    private ServiceCenter serviceCenter;

    public Analytics() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMetrics() { return metrics; }
    public void setMetrics(String metrics) { this.metrics = metrics; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public ServiceCenter getServiceCenter() { return serviceCenter; }
    public void setServiceCenter(ServiceCenter serviceCenter) { this.serviceCenter = serviceCenter; }
}
