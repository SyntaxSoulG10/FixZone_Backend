package com.fixzone.fixzon_backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "reports")
public class Report {

    @Id
    @Column(name = "report_id")
    private UUID id;

    private String name;
    private LocalDate date;
    private String type;
    private String size;
    private String downloadUrl;

    @Column(columnDefinition="TEXT")
    private String fileContentBase64;

    @Column(columnDefinition="TEXT")
    private String description;

    private java.time.LocalDateTime createdAt;

    public Report() {
        this.id = UUID.randomUUID();
        this.createdAt = java.time.LocalDateTime.now();
    }

    public Report(String name, LocalDate date, String type, String size, String downloadUrl) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.date = date;
        this.type = type;
        this.size = size;
        this.downloadUrl = downloadUrl;
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID();
        if (this.createdAt == null) this.createdAt = java.time.LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    public String getFileContentBase64() { return fileContentBase64; }
    public void setFileContentBase64(String fileContentBase64) { this.fileContentBase64 = fileContentBase64; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
}
