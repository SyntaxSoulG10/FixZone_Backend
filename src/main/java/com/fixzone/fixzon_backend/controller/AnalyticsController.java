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
    public ResponseEntity<AnalyticsDTO> getCompanyAnalytics(@PathVariable String companyCode) {
        AnalyticsDTO analyticsData = analyticsService.getCompanyAnalytics(companyCode);
        return ResponseEntity.ok(analyticsData);
    }

    @GetMapping("/current")
    public ResponseEntity<AnalyticsDTO> getCurrentOwnerAnalytics() {
        // Hardcoded for development until authentication is finished
        AnalyticsDTO analyticsData = analyticsService.getCompanyAnalytics("FIX001");
        return ResponseEntity.ok(analyticsData);
    }
}
