package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.AnalyticsDTO;
import com.fixzone.fixzon_backend.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

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
            // Hardcoded for development until authentication is finished
            AnalyticsDTO analyticsData = analyticsService.getCompanyAnalytics("FIX001", centerId, startDate, endDate, period);
            return ResponseEntity.ok(analyticsData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch current analytics: " + e.getMessage());
        }
    }
}
