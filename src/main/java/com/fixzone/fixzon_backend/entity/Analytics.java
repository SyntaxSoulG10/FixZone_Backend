package com.fixzone.fixzon_backend.entity;

import com.fixzone.fixzon_backend.model.ServiceCenter;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "analytics")
public class Analytics {

    @Id
    @Column(name = "analytics_id")
    private UUID id;

    private String metrics; 
    private LocalDate date;

    @ManyToOne
    @JoinColumn(name = "service_center_id")
    private ServiceCenter serviceCenter;

    public Analytics() {
        this.id = UUID.randomUUID();
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getMetrics() { return metrics; }
    public void setMetrics(String metrics) { this.metrics = metrics; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public ServiceCenter getServiceCenter() { return serviceCenter; }
    public void setServiceCenter(ServiceCenter serviceCenter) { this.serviceCenter = serviceCenter; }
}
