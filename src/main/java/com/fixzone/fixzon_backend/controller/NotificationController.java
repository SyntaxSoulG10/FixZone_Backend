package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.NotificationDTO;
import com.fixzone.fixzon_backend.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@Tag(name = "Notifications", description = "In-app notification and alert APIs")
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getNotifications(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        String email = (String) authentication.getPrincipal();
        return ResponseEntity.ok(notificationService.getNotificationsForUser(email));
    }

    @GetMapping("/archived")
    public ResponseEntity<List<NotificationDTO>> getArchivedNotifications(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        String email = (String) authentication.getPrincipal();
        return ResponseEntity.ok(notificationService.getArchivedNotificationsForUser(email));
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<NotificationDTO> archiveNotification(@PathVariable UUID id, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        String email = (String) authentication.getPrincipal();
        return ResponseEntity.ok(notificationService.archiveNotification(id, email));
    }

    @PatchMapping("/{id}/unarchive")
    public ResponseEntity<NotificationDTO> unarchiveNotification(@PathVariable UUID id, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        String email = (String) authentication.getPrincipal();
        return ResponseEntity.ok(notificationService.unarchiveNotification(id, email));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationDTO> markAsRead(@PathVariable UUID id, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        String email = (String) authentication.getPrincipal();
        return ResponseEntity.ok(notificationService.markAsRead(id, email));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        String email = (String) authentication.getPrincipal();
        notificationService.markAllAsRead(email);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable UUID id, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        String email = (String) authentication.getPrincipal();
        notificationService.deleteNotification(id, email);
        return ResponseEntity.noContent().build();
    }
}
