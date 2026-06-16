package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.NotificationDTO;
import com.fixzone.fixzon_backend.model.Notification;
import com.fixzone.fixzon_backend.model.User;
import com.fixzone.fixzon_backend.repository.NotificationRepository;
import com.fixzone.fixzon_backend.repository.UserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public List<NotificationDTO> getNotificationsForUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(user.getUserId()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
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
        List<Notification> unread = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(user.getUserId())
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
}
