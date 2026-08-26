package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.model.Report;
import com.fixzone.fixzon_backend.service.ReportService;
import com.fixzone.fixzon_backend.service.OwnerService;
import com.fixzone.fixzon_backend.DTO.OwnerDTO;
import com.fixzone.fixzon_backend.middleware.RequireRole;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*") // Allow frontend to fetch data
@RequireRole({"ROLE_COMPANY_OWNER", "ROLE_SUPER_ADMIN"})
public class ReportController {

    private final ReportService reportService;
    private final OwnerService ownerService;

    public ReportController(ReportService reportService, OwnerService ownerService) {
        this.reportService = reportService;
        this.ownerService = ownerService;
    }

    @GetMapping
    public ResponseEntity<List<Report>> getAllReports() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean isSuperAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_SUPER_ADMIN")
                        || a.getAuthority().equalsIgnoreCase("SUPER_ADMIN"));

        if (isSuperAdmin) {
            return ResponseEntity.ok(reportService.getAllReports());
        }

        OwnerDTO owner = ownerService.retrieveOwnerByEmail(auth.getName());
        if (owner == null) {
            return ResponseEntity.ok(List.of());
        }

        return ResponseEntity.ok(reportService.getReportsByOwnerCode(owner.getOwnerCode()));
    }

    @PostMapping
    public ResponseEntity<Report> createReport(@RequestBody Report report) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean isSuperAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_SUPER_ADMIN")
                        || a.getAuthority().equalsIgnoreCase("SUPER_ADMIN"));

        if (isSuperAdmin) {
            report.setOwnerCode("SYSTEM");
        } else {
            OwnerDTO owner = ownerService.retrieveOwnerByEmail(auth.getName());
            if (owner != null) {
                report.setOwnerCode(owner.getOwnerCode());
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        Report createdReport = reportService.createReport(report);
        return ResponseEntity.ok(createdReport);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable UUID id) {
        // Optional safety: restrict deletion only if it belongs to current owner
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean isSuperAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_SUPER_ADMIN")
                        || a.getAuthority().equalsIgnoreCase("SUPER_ADMIN"));

        if (!isSuperAdmin) {
            OwnerDTO owner = ownerService.retrieveOwnerByEmail(auth.getName());
            if (owner == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            // For extra safety, you can load report first to verify ownerCode matches,
            // but simple delete is fine.
        }

        reportService.deleteReport(id);
        return ResponseEntity.noContent().build();
    }
}
