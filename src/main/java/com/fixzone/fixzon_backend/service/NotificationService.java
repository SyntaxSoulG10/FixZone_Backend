package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.NotificationDTO;
import com.fixzone.fixzon_backend.model.Notification;
import com.fixzone.fixzon_backend.model.User;
import com.fixzone.fixzon_backend.model.Manager;
import com.fixzone.fixzon_backend.model.MobileDeviceToken;
import com.fixzone.fixzon_backend.repository.MobileDeviceTokenRepository;
import com.fixzone.fixzon_backend.repository.NotificationRepository;
import com.fixzone.fixzon_backend.repository.UserRepository;
import com.fixzone.fixzon_backend.repository.ManagerRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service

public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ManagerRepository managerRepository;
    private final MobileDeviceTokenRepository mobileDeviceTokenRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               ManagerRepository managerRepository,
                               MobileDeviceTokenRepository mobileDeviceTokenRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.managerRepository = managerRepository;
        this.mobileDeviceTokenRepository = mobileDeviceTokenRepository;
    }

    public List<NotificationDTO> getNotificationsForUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        return notificationRepository.findByRecipientUserIdAndIsArchivedFalseOrderByCreatedAtDesc(user.getUserId()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<NotificationDTO> getArchivedNotificationsForUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        return notificationRepository.findByRecipientUserIdAndIsArchivedTrueOrderByCreatedAtDesc(user.getUserId()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public NotificationDTO archiveNotification(UUID notificationId, String email) {
        Notification note = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (!note.getRecipient().getEmail().equalsIgnoreCase(email)) {
            throw new RuntimeException("Unauthorized access to notification");
        }
        note.setArchived(true);
        return convertToDTO(notificationRepository.save(note));
    }

    @Transactional
    public NotificationDTO unarchiveNotification(UUID notificationId, String email) {
        Notification note = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (!note.getRecipient().getEmail().equalsIgnoreCase(email)) {
            throw new RuntimeException("Unauthorized access to notification");
        }
        note.setArchived(false);
        return convertToDTO(notificationRepository.save(note));
    }

    @Transactional
    public NotificationDTO markAsRead(UUID notificationId, String email) {
        Notification note = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (!note.getRecipient().getEmail().equalsIgnoreCase(email)) {
            throw new RuntimeException("Unauthorized access to notification");
        }
        note.setRead(true);
        return convertToDTO(notificationRepository.save(note));
    }

    @Transactional
    public void markAllAsRead(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Notification> unread = notificationRepository.findByRecipientUserIdAndIsArchivedFalseOrderByCreatedAtDesc(user.getUserId())
                .stream()
                .filter(n -> !n.isRead())
                .collect(Collectors.toList());
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public void deleteNotification(UUID notificationId, String email) {
        Notification note = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (!note.getRecipient().getEmail().equalsIgnoreCase(email)) {
            throw new RuntimeException("Unauthorized access to notification");
        }
        notificationRepository.delete(note);
    }

    @Transactional
    public void createNotification(User recipient, String title, String message, String type, String targetUrl) {
        if (recipient == null) return;
        Notification note = new Notification();
        note.setRecipient(recipient);
        note.setTitle(title);
        note.setMessage(message);
        note.setType(type);
        note.setTargetUrl(targetUrl);
        notificationRepository.save(note);

        sendExpoPushNotification(recipient.getUserId(), title, message, targetUrl);
    }

    public void sendExpoPushNotification(UUID userId, String title, String message, String targetUrl) {
        try {
            List<MobileDeviceToken> tokens = mobileDeviceTokenRepository.findByUserIdAndActiveTrue(userId);
            if (tokens == null || tokens.isEmpty()) return;

            RestTemplate restTemplate = new RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            for (MobileDeviceToken dt : tokens) {
                if (dt.getToken() == null || dt.getToken().isBlank()) continue;

                Map<String, Object> body = new HashMap<>();
                body.put("to", dt.getToken());
                body.put("sound", "default");
                body.put("title", title);
                body.put("body", message);
                if (targetUrl != null && !targetUrl.isBlank()) {
                    body.put("data", Map.of("targetUrl", targetUrl));
                }

                org.springframework.http.HttpEntity<Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(body, headers);
                org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity("https://exp.host/--/api/v2/push/send", entity, String.class);
                System.out.println(">>> Expo Push API Ticket Response for user " + userId + ": " + response.getBody());
            }
        } catch (Exception e) {
            System.err.println("Expo Push Notification error: " + e.getMessage());
        }
    }

    public void createNotificationSafe(User recipient, String title, String message, String type, String targetUrl) {
        try {
            createNotification(recipient, title, message, type, targetUrl);
        } catch (Exception e) {
            System.err.println("Failed to create notification safely: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional
    public void broadcastNotification(List<? extends User> recipients, String title, String message, String type, String targetUrl) {
        if (recipients == null || recipients.isEmpty()) return;
        List<Notification> notes = new ArrayList<>();
        for (User recipient : recipients) {
            if (recipient == null) continue;
            Notification note = new Notification();
            note.setRecipient(recipient);
            note.setTitle(title);
            note.setMessage(message);
            note.setType(type);
            note.setTargetUrl(targetUrl);
            notes.add(note);
        }
        notificationRepository.saveAll(notes);
    }

    public void broadcastNotificationSafe(List<? extends User> recipients, String title, String message, String type, String targetUrl) {
        try {
            broadcastNotification(recipients, title, message, type, targetUrl);
        } catch (Exception e) {
            System.err.println("Failed to broadcast notification safely: " + e.getMessage());
            e.printStackTrace();
        }
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

    @Transactional
    public void notifyCenterManagers(UUID centerId, String title, String message, String type, String targetUrl) {
        if (centerId == null) return;
        try {
            List<Manager> managers = managerRepository.findByManagedCenterId(centerId);
            if (managers != null && !managers.isEmpty()) {
                for (Manager manager : managers) {
                    createNotification(manager, title, message, type, targetUrl);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to notify center managers: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void notifyCenterManagersSafe(UUID centerId, String title, String message, String type, String targetUrl) {
        try {
            notifyCenterManagers(centerId, title, message, type, targetUrl);
        } catch (Exception e) {
            System.err.println("Failed to notify center managers safely: " + e.getMessage());
        }
    }
}
