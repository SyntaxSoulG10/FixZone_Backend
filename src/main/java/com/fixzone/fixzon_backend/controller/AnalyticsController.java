package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.AnalyticsDTO;
import com.fixzone.fixzon_backend.service.AnalyticsService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.context.SecurityContextHolder;
import com.fixzone.fixzon_backend.service.OwnerService;
import com.fixzone.fixzon_backend.DTO.OwnerDTO;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final OwnerService ownerService;

    public AnalyticsController(AnalyticsService analyticsService, OwnerService ownerService) {
        this.analyticsService = analyticsService;
        this.ownerService = ownerService;
    }

    @GetMapping("/company/{companyCode}")
    public ResponseEntity<AnalyticsDTO> getCompanyAnalytics(
            @PathVariable String companyCode,
            @RequestParam(required = false) String centerId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "monthly") String period) {
        try {
            AnalyticsDTO analyticsData = analyticsService.getCompanyAnalytics(companyCode, centerId, startDate, endDate, period);
            return ResponseEntity.ok(analyticsData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch analytics: " + e.getMessage());
        }
    }

    @GetMapping("/current")
    public ResponseEntity<AnalyticsDTO> getCurrentOwnerAnalytics(
            @RequestParam(required = false) String centerId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "monthly") String period) {
        try {
            org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            
            boolean isSuperAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));

            if (isSuperAdmin) {
                AnalyticsDTO analyticsData = analyticsService.getCompanyAnalytics("SYSTEM", centerId, startDate, endDate, period);
                return ResponseEntity.ok(analyticsData);
            }

            // Get the current authenticated user's email from the SecurityContext
            String email = (String) auth.getPrincipal();
            
            // Retrieve the owner to get their ownerCode
            OwnerDTO owner = ownerService.retrieveOwnerByEmail(email);
            if (owner == null) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).build();
            }

            AnalyticsDTO analyticsData = analyticsService.getCompanyAnalytics(owner.getOwnerCode(), centerId, startDate, endDate, period);
            return ResponseEntity.ok(analyticsData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch current analytics: " + e.getMessage());
        }
    }
}
