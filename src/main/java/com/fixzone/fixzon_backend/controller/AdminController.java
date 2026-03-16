package com.fixzone.fixzon_backend.controller;
 
import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.model.User;
import com.fixzone.fixzon_backend.entity.Notification;
import com.fixzone.fixzon_backend.service.AdminService;
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
 
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }
 
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(adminService.getSystemStats());
    }
 
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }
 
    @PostMapping("/users/{id}/status")
    public ResponseEntity<User> updateUserStatus(
            @PathVariable UUID id, @RequestParam String status) {
        return ResponseEntity.ok(adminService.updateUserStatus(id, status));
    }
 
    @GetMapping("/service-centers")
    public ResponseEntity<List<ServiceCenter>> getAllServiceCenters() {
        return ResponseEntity.ok(adminService.getAllServiceCenters());
    }
 
    @GetMapping("/service-centers/pending")
    public ResponseEntity<List<ServiceCenter>> getPendingServiceCenters() {
        return ResponseEntity.ok(adminService.getPendingServiceCenters());
    }
 
    @PostMapping("/service-centers/{id}/approve")
    public ResponseEntity<ServiceCenter> approveServiceCenter(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.approveServiceCenter(id));
    }
 
    @PostMapping("/service-centers/{id}/reject")
    public ResponseEntity<ServiceCenter> rejectServiceCenter(
            @PathVariable UUID id, @RequestParam String reason) {
        return ResponseEntity.ok(adminService.rejectServiceCenter(id, reason));
    }
 
    @PostMapping("/service-centers/{id}/status")
    public ResponseEntity<ServiceCenter> updateServiceCenterStatus(
            @PathVariable UUID id, @RequestParam String status) {
        return ResponseEntity.ok(adminService.updateServiceCenterStatus(id, status));
    }
 
    @GetMapping("/notifications")
    public ResponseEntity<List<Notification>> getNotifications() {
        return ResponseEntity.ok(adminService.getAdminNotifications());
    }
}
