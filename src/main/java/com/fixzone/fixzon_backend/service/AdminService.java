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
import com.fixzone.fixzon_backend.repository.SubscriptionRepository;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
import com.fixzone.fixzon_backend.DTO.SubscriptionDTO;
import com.fixzone.fixzon_backend.model.Subscription;
import com.fixzone.fixzon_backend.model.Owner;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final SubscriptionRepository subscriptionRepository;
    private final OwnerRepository ownerRepository;

    public AdminService(ServiceCenterRepository serviceCenterRepository, 
                        UserRepository userRepository,
                        NotificationRepository notificationRepository,
                        SubscriptionRepository subscriptionRepository,
                        OwnerRepository ownerRepository) {
        this.serviceCenterRepository = serviceCenterRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.ownerRepository = ownerRepository;
    }

    // --- Service Center Management ---

    public List<ServiceCenterDTO> getPendingServiceCenters() {
        return serviceCenterRepository.findByStatus("PENDING").stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
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

    @Transactional
    public ServiceCenterDTO rejectServiceCenter(UUID id, String reason) {
        Objects.requireNonNull(id, "ID must not be null");
        ServiceCenter sc = serviceCenterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service Center not found"));
        sc.setStatus("REJECTED");
        sc.setIsActive(false);
        sc.setRejectionReason(reason);
        
        createNotification(sc.getOwner(), "Registration Rejected", 
            "Your registration for '" + sc.getName() + "' was rejected. Reason: " + reason, "WARNING");
            
        return convertToDTO(serviceCenterRepository.save(sc));
    }

    @Transactional
    public ServiceCenterDTO updateServiceCenterStatus(UUID id, String status) {
        Objects.requireNonNull(id, "ID must not be null");
        ServiceCenter sc = serviceCenterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service Center not found"));
        sc.setStatus(status); // SUSPENDED, ACTIVE
        return convertToDTO(serviceCenterRepository.save(sc));
    }

    // --- User Account Management & Platform Security ---
    // Methods for managing global user access, status transitions, and administrative oversight.

    @Transactional
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

    // --- Subscription Management ---

    public List<SubscriptionDTO> getSubscriptions(String status) {
        List<Subscription> subs = (status == null || status.equalsIgnoreCase("ALL"))
                ? subscriptionRepository.findAllByOrderByStartDateDesc()
                : subscriptionRepository.findByStatus(status.toUpperCase());

        // Optimize: Bulk fetch owners to get company names
        List<UUID> ownerIds = subs.stream()
                .map(s -> s.getOwner() != null ? s.getOwner().getUserId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<UUID, String> companyNames = ownerRepository.findAllById(ownerIds).stream()
                .collect(Collectors.toMap(Owner::getUserId, Owner::getCompanyName, (a, b) -> a));

        return subs.stream()
                .map(s -> convertToDTO(s, companyNames))
                .collect(Collectors.toList());
    }

    public SubscriptionDTO getSubscriptionById(UUID id) {
        Subscription sub = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        
        String companyName = "N/A";
        if (sub.getOwner() != null) {
            companyName = ownerRepository.findById(sub.getOwner().getUserId())
                    .map(Owner::getCompanyName).orElse("N/A");
        }
        
        return convertToDTO(sub, Map.of(sub.getOwner().getUserId(), companyName));
    }

    @Transactional
    public SubscriptionDTO updateSubscriptionStatus(UUID id, String status) {
        Subscription sub = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        sub.setStatus(status.toUpperCase());
        
        // Notify owner of the change
        createNotification(sub.getOwner(), "Subscription Status Updated", 
            "Your subscription status has been changed to " + status + ".", "INFO");
            
        Subscription saved = subscriptionRepository.save(sub);
        
        // Return DTO (company name lookup)
        String companyName = "N/A";
        if (saved.getOwner() != null) {
            companyName = ownerRepository.findById(saved.getOwner().getUserId())
                    .map(Owner::getCompanyName).orElse("N/A");
        }
        return convertToDTO(saved, Map.of(saved.getOwner().getUserId(), companyName));
    }

    // --- Mapping Helpers ---

    private ServiceCenterDTO convertToDTO(ServiceCenter sc) {
        Objects.requireNonNull(sc, "ServiceCenter must not be null");
        ServiceCenterDTO dto = new ServiceCenterDTO();
        BeanUtils.copyProperties(sc, dto);
        if (sc.getOwner() != null) {
            dto.setOwnerId(sc.getOwner().getUserId());
        }
        return dto;
    }

    private UserDTO convertToDTO(User user) {
        Objects.requireNonNull(user, "User must not be null");
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }

    private NotificationDTO convertToDTO(Notification note) {
        Objects.requireNonNull(note, "Notification must not be null");
        NotificationDTO dto = new NotificationDTO();
        BeanUtils.copyProperties(note, dto);
        if (note.getRecipient() != null) {
            dto.setRecipientId(note.getRecipient().getUserId());
        }
        return dto;
    }

    private SubscriptionDTO convertToDTO(Subscription sub, Map<UUID, String> companyNames) {
        Objects.requireNonNull(sub, "Subscription must not be null");
        SubscriptionDTO dto = new SubscriptionDTO();
        BeanUtils.copyProperties(sub, dto);
        if (sub.getOwner() != null) {
            dto.setOwnerId(sub.getOwner().getUserId());
            dto.setOwnerName(sub.getOwner().getFullName());
            dto.setCompanyName(companyNames.getOrDefault(sub.getOwner().getUserId(), "N/A"));
        }
        return dto;
    }
}
