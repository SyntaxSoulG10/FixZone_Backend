package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.model.Report;
import com.fixzone.fixzon_backend.repository.ReportRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ReportService {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ImageKitService imageKitService;

    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    public Report createReport(Report report) {
        if (report.getDate() == null) {
            report.setDate(LocalDate.now());
        }
        if (report.getSize() == null || report.getSize().isEmpty()) {
            report.setSize(String.format("%.1f MB", Math.random() * 5 + 0.1));
        }

        if (report.getFileContentBase64() != null && !report.getFileContentBase64().isEmpty()) {
            boolean isOperations = report.getType() != null && report.getType().toUpperCase().contains("OPERATIONS");
            if (!isOperations) {
                try {
                    String uploadedUrl = imageKitService.uploadImage(report.getFileContentBase64(), report.getName().replaceAll("\\s+", "_"));
                    if (uploadedUrl != null) {
                        report.setDownloadUrl(uploadedUrl);
                        // Clear the base64 content so we don't store it in the database
                        report.setFileContentBase64(null);
                    }
                } catch (Exception e) {
                    // If upload fails, just continue, fallback behavior applies
                    e.printStackTrace();
                }
            }
        }

        if (report.getDownloadUrl() == null || report.getDownloadUrl().isEmpty()) {
            report.setDownloadUrl("/downloads/generated-" + System.currentTimeMillis());
        }
        return reportRepository.save(report);
    }

    public Report updateReport(UUID id, Report updatedReport) {
        return reportRepository.findById(id).map(existing -> {
            if (updatedReport.getName() != null) existing.setName(updatedReport.getName());
            if (updatedReport.getDate() != null) existing.setDate(updatedReport.getDate());
            if (updatedReport.getType() != null) existing.setType(updatedReport.getType());
            if (updatedReport.getFileContentBase64() != null) existing.setFileContentBase64(updatedReport.getFileContentBase64());
            return reportRepository.save(existing);
        }).orElseGet(() -> {
            updatedReport.setId(id);
            return reportRepository.save(updatedReport);
        });
    }

    public void deleteReport(UUID id) {
        reportRepository.deleteById(id);
    }
}
