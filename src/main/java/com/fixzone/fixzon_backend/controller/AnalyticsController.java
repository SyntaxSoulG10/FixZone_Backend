package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.AnalyticsDTO;
import com.fixzone.fixzon_backend.service.AnalyticsService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.context.SecurityContextHolder;
import com.fixzone.fixzon_backend.service.OwnerService;
import com.fixzone.fixzon_backend.DTO.OwnerDTO;
import com.fixzone.fixzon_backend.service.ManagerService;
import com.fixzone.fixzon_backend.DTO.ManagerDTO;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;
    private final OwnerService ownerService;
    private final ManagerService managerService;

    public AnalyticsController(AnalyticsService analyticsService, OwnerService ownerService, ManagerService managerService) {
        this.analyticsService = analyticsService;
        this.ownerService = ownerService;
        this.managerService = managerService;
    }

    @GetMapping("/company/{companyCode}")
    public ResponseEntity<AnalyticsDTO> getCompanyAnalytics(
            @PathVariable String companyCode,
            @RequestParam(required = false) String centerId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "monthly") String period) {
        AnalyticsDTO analyticsData = analyticsService.getCompanyAnalytics(companyCode, centerId, startDate, endDate, period);
        return ResponseEntity.ok(analyticsData);
    }

    @GetMapping("/current")
    public ResponseEntity<AnalyticsDTO> getCurrentOwnerAnalytics(
            @RequestParam(required = false) String centerId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "monthly") String period) {
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }

        boolean isSuperAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_SUPER_ADMIN") || a.getAuthority().equalsIgnoreCase("SUPER_ADMIN"));

        if (isSuperAdmin) {
            AnalyticsDTO analyticsData = analyticsService.getCompanyAnalytics("SYSTEM", centerId, startDate, endDate, period);
            return ResponseEntity.ok(analyticsData);
        }

        String email = auth.getName();

        boolean isManager = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_SERVICE_MANAGER") || a.getAuthority().equalsIgnoreCase("MANAGER") || a.getAuthority().equalsIgnoreCase("ROLE_MANAGER"));

        if (isManager) {
            ManagerDTO manager = managerService.getManagerByEmail(email);
            if (manager == null || manager.getManagedCenterId() == null) {
                return ResponseEntity.ok(new AnalyticsDTO());
            }
            // For a Service Manager, we override the centerId to their assigned center
            AnalyticsDTO analyticsData = analyticsService.getCompanyAnalytics("SYSTEM", manager.getManagedCenterId().toString(), startDate, endDate, period);
            return ResponseEntity.ok(analyticsData);
        }

        // Retrieve the owner to get their ownerCode
        OwnerDTO owner = ownerService.retrieveOwnerByEmail(email);
        if (owner == null) {
            return ResponseEntity.ok(new AnalyticsDTO());
        }

        AnalyticsDTO analyticsData = analyticsService.getCompanyAnalytics(owner.getOwnerCode(), centerId, startDate, endDate, period);
        return ResponseEntity.ok(analyticsData);
    }
}
