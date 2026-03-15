package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.entity.ServiceCenter;
import com.fixzone.fixzon_backend.entity.User;
import com.fixzone.fixzon_backend.entity.Notification;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.UserRepository;
import com.fixzone.fixzon_backend.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import java.util.List;

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
        return serviceCenterRepository.findByStatus("PENDING");
    }

    public ServiceCenter approveServiceCenter(Long id) {
        ServiceCenter sc = serviceCenterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service Center not found"));
        sc.setStatus("APPROVED");
        
        // Notify owner
        createNotification(sc.getOwner(), "Registration Approved", 
            "Your service center '" + sc.getName() + "' has been approved.", "SUCCESS");
            
        return serviceCenterRepository.save(sc);
    }

    public ServiceCenter rejectServiceCenter(Long id, String reason) {
        ServiceCenter sc = serviceCenterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service Center not found"));
        sc.setStatus("REJECTED");
        
        createNotification(sc.getOwner(), "Registration Rejected", 
            "Your registration for '" + sc.getName() + "' was rejected. Reason: " + reason, "WARNING");
            
        return serviceCenterRepository.save(sc);
    }

    public ServiceCenter updateServiceCenterStatus(Long id, String status) {
        ServiceCenter sc = serviceCenterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service Center not found"));
        sc.setStatus(status); // SUSPENDED, ACTIVE
        return serviceCenterRepository.save(sc);
    }

    // --- User Account Management ---

    public User updateUserStatus(Long id, String status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(status); // Active, Suspended
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // --- Dashboard & Stats ---

    public java.util.Map<String, Object> getSystemStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalServiceCenters", serviceCenterRepository.count());
        stats.put("pendingRegistrations", serviceCenterRepository.findByStatus("PENDING").size());
        // For simplicity, returning counts. In a real app, this would be more complex.
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
        // Here we might fetch global notifications or ones for the Super Admin
        return notificationRepository.findAll(); 
    }

    public List<ServiceCenter> getAllServiceCenters() {
        return serviceCenterRepository.findAll();
    }
}
