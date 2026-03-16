package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.model.User;
import com.fixzone.fixzon_backend.entity.Notification;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.UserRepository;
import com.fixzone.fixzon_backend.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@Service
public class AdminService {

    private final ServiceCenterRepository serviceCenterRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public AdminService(ServiceCenterRepository serviceCenterRepository, 
                        UserRepository userRepository,
                        NotificationRepository notificationRepository) {
        this.serviceCenterRepository = serviceCenterRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    // --- Service Center Management ---

    public List<ServiceCenter> getPendingServiceCenters() {
        // Assuming model.ServiceCenter has a status field now
        return serviceCenterRepository.findAll().stream()
                .filter(sc -> "PENDING".equals(sc.getStatus()))
                .toList();
    }

    public ServiceCenter approveServiceCenter(UUID id) {
        ServiceCenter sc = serviceCenterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service Center not found"));
        sc.setStatus("APPROVED");
        sc.setIsActive(true);
        
        // Notify owner
        createNotification(sc.getOwner(), "Registration Approved", 
            "Your service center '" + sc.getName() + "' has been approved.", "SUCCESS");
            
        return serviceCenterRepository.save(sc);
    }

    public ServiceCenter rejectServiceCenter(UUID id, String reason) {
        ServiceCenter sc = serviceCenterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service Center not found"));
        sc.setStatus("REJECTED");
        sc.setIsActive(false);
        
        createNotification(sc.getOwner(), "Registration Rejected", 
            "Your registration for '" + sc.getName() + "' was rejected. Reason: " + reason, "WARNING");
            
        return serviceCenterRepository.save(sc);
    }

    public ServiceCenter updateServiceCenterStatus(UUID id, String status) {
        ServiceCenter sc = serviceCenterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service Center not found"));
        sc.setStatus(status); // SUSPENDED, ACTIVE
        return serviceCenterRepository.save(sc);
    }

    // --- User Account Management ---

    public User updateUserStatus(UUID id, String status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(status); // Active, Suspended
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // --- Dashboard & Stats ---

    public Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalServiceCenters", serviceCenterRepository.count());
        stats.put("pendingRegistrations", getPendingServiceCenters().size());
        return stats;
    }

    // --- Monitoring & Notifications ---

    private void createNotification(User recipient, String title, String message, String type) {
        if (recipient == null) return;
        Notification note = new Notification();
        note.setRecipient(recipient);
        note.setTitle(title);
        note.setMessage(message);
        note.setType(type);
        notificationRepository.save(note);
    }
    
    public List<Notification> getAdminNotifications() {
        return notificationRepository.findAll(); 
    }

    public List<ServiceCenter> getAllServiceCenters() {
        return serviceCenterRepository.findAll();
    }
}
