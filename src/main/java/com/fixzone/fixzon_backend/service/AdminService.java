package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.NotificationDTO;
import com.fixzone.fixzon_backend.DTO.ServiceCenterDTO;
import com.fixzone.fixzon_backend.DTO.UserDTO;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.model.User;
import com.fixzone.fixzon_backend.model.Notification;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.UserRepository;
import com.fixzone.fixzon_backend.repository.NotificationRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public List<ServiceCenterDTO> getPendingServiceCenters() {
        return serviceCenterRepository.findAll().stream()
                .filter(sc -> "PENDING".equals(sc.getStatus()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ServiceCenterDTO approveServiceCenter(UUID id) {
        Objects.requireNonNull(id, "ID must not be null");
        ServiceCenter sc = serviceCenterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service Center not found"));
        sc.setStatus("APPROVED");
        sc.setIsActive(true);
        
        // Notify owner
        createNotification(sc.getOwner(), "Registration Approved", 
            "Your service center '" + sc.getName() + "' has been approved.", "SUCCESS");
            
        return convertToDTO(serviceCenterRepository.save(sc));
    }

    public ServiceCenterDTO rejectServiceCenter(UUID id, String reason) {
        Objects.requireNonNull(id, "ID must not be null");
        ServiceCenter sc = serviceCenterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service Center not found"));
        sc.setStatus("REJECTED");
        sc.setIsActive(false);
        
        createNotification(sc.getOwner(), "Registration Rejected", 
            "Your registration for '" + sc.getName() + "' was rejected. Reason: " + reason, "WARNING");
            
        return convertToDTO(serviceCenterRepository.save(sc));
    }

    public ServiceCenterDTO updateServiceCenterStatus(UUID id, String status) {
        Objects.requireNonNull(id, "ID must not be null");
        ServiceCenter sc = serviceCenterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service Center not found"));
        sc.setStatus(status); // SUSPENDED, ACTIVE
        return convertToDTO(serviceCenterRepository.save(sc));
    }

    // --- User Account Management ---

    public UserDTO updateUserStatus(UUID id, String status) {
        Objects.requireNonNull(id, "ID must not be null");
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(status); // Active, Suspended
        return convertToDTO(userRepository.save(user));
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
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
    
    public List<NotificationDTO> getAdminNotifications() {
        return notificationRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList()); 
    }

    public List<ServiceCenterDTO> getAllServiceCenters() {
        return serviceCenterRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // --- Mapping Helpers ---

    private ServiceCenterDTO convertToDTO(ServiceCenter sc) {
        ServiceCenterDTO dto = new ServiceCenterDTO();
        BeanUtils.copyProperties(sc, dto);
        if (sc.getOwner() != null) {
            dto.setOwnerId(sc.getOwner().getUserId());
        }
        return dto;
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }

    private NotificationDTO convertToDTO(Notification note) {
        NotificationDTO dto = new NotificationDTO();
        BeanUtils.copyProperties(note, dto);
        if (note.getRecipient() != null) {
            dto.setRecipientId(note.getRecipient().getUserId());
        }
        return dto;
    }
}

