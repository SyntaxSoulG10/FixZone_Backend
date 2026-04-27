package com.fixzone.fixzon_backend.controller;
 
import com.fixzone.fixzon_backend.DTO.NotificationDTO;
import com.fixzone.fixzon_backend.DTO.ServiceCenterDTO;
import com.fixzone.fixzon_backend.DTO.SuperAdminAnalyticsDTO;
import com.fixzone.fixzon_backend.DTO.SubscriptionDTO;
import com.fixzone.fixzon_backend.DTO.UserDTO;
import com.fixzone.fixzon_backend.service.AdminService;
import com.fixzone.fixzon_backend.service.SuperAdminAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
 
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*") // Allow frontend to connect
public class AdminController {
 
    private final AdminService adminService;
    private final SuperAdminAnalyticsService superAdminAnalyticsService;

    public AdminController(AdminService adminService, SuperAdminAnalyticsService superAdminAnalyticsService) {
        this.adminService = adminService;
        this.superAdminAnalyticsService = superAdminAnalyticsService;
    }
 
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(adminService.getSystemStats());
    }

    @GetMapping("/analytics")
    public ResponseEntity<SuperAdminAnalyticsDTO> getSuperAdminAnalytics() {
        return ResponseEntity.ok(superAdminAnalyticsService.getAnalytics());
    }
 
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }
 
    @PostMapping("/users/{id}/status")
    public ResponseEntity<UserDTO> updateUserStatus(
            @PathVariable UUID id, @RequestParam String status) {
        return ResponseEntity.ok(adminService.updateUserStatus(id, status));
    }
 
    @GetMapping("/service-centers")
    public ResponseEntity<List<ServiceCenterDTO>> getAllServiceCenters() {
        return ResponseEntity.ok(adminService.getAllServiceCenters());
    }
 
    @GetMapping("/service-centers/pending")
    public ResponseEntity<List<ServiceCenterDTO>> getPendingServiceCenters() {
        return ResponseEntity.ok(adminService.getPendingServiceCenters());
    }
 
    @PostMapping("/service-centers/{id}/approve")
    public ResponseEntity<ServiceCenterDTO> approveServiceCenter(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.approveServiceCenter(id));
    }
 
    @PostMapping("/service-centers/{id}/reject")
    public ResponseEntity<ServiceCenterDTO> rejectServiceCenter(
            @PathVariable UUID id, @RequestParam String reason) {
        return ResponseEntity.ok(adminService.rejectServiceCenter(id, reason));
    }
 
    @PostMapping("/service-centers/{id}/status")
    public ResponseEntity<ServiceCenterDTO> updateServiceCenterStatus(
            @PathVariable UUID id, @RequestParam String status) {
        return ResponseEntity.ok(adminService.updateServiceCenterStatus(id, status));
    }
 
    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationDTO>> getNotifications() {
        return ResponseEntity.ok(adminService.getAdminNotifications());
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<List<SubscriptionDTO>> getSubscriptions(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(adminService.getSubscriptions(status));
    }

    @GetMapping("/subscriptions/{id}")
    public ResponseEntity<SubscriptionDTO> getSubscriptionById(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.getSubscriptionById(id));
    }

    @PatchMapping("/subscriptions/{id}/status")
    public ResponseEntity<SubscriptionDTO> updateSubscriptionStatus(
            @PathVariable UUID id, @RequestParam String status) {
        return ResponseEntity.ok(adminService.updateSubscriptionStatus(id, status));
    }
}
