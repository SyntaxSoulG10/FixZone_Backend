package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.UserDTO;
import com.fixzone.fixzon_backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        try {
            return ResponseEntity.ok(userService.getAllUsers());
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch users: " + e.getMessage());
        }
    }

    @PutMapping("/{userId}/profile-image")
    public ResponseEntity<?> updateProfileImage(@PathVariable UUID userId, @RequestBody Map<String, String> payload) {
        try {
            String imageUrl = payload.get("imageUrl");
            userService.updateProfileImage(userId, imageUrl);
            return ResponseEntity.ok().body(Map.of("message", "Profile image updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{userId}/profile")
    public ResponseEntity<?> updateProfile(@PathVariable UUID userId, @RequestBody Map<String, String> payload) {
        try {
            String fullName = payload.get("fullName");
            String phone = payload.get("phone");
            userService.updateProfile(userId, fullName, phone);
            return ResponseEntity.ok().body(Map.of("message", "Profile updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
