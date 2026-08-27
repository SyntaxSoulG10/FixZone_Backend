package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.model.MobileDeviceToken;
import com.fixzone.fixzon_backend.model.User;
import com.fixzone.fixzon_backend.repository.MobileDeviceTokenRepository;
import com.fixzone.fixzon_backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/mobile/notifications")
@CrossOrigin(origins = "*")
public class MobileNotificationController {

    private final MobileDeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    public MobileNotificationController(MobileDeviceTokenRepository deviceTokenRepository, UserRepository userRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/register-token")
    public ResponseEntity<?> registerToken(Authentication authentication, @RequestBody Map<String, String> request) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        String token = request.get("token");
        String platform = request.getOrDefault("platform", "android");

        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token is required"));
        }

        Optional<MobileDeviceToken> existingOpt = deviceTokenRepository.findByToken(token);
        MobileDeviceToken deviceToken;

        if (existingOpt.isPresent()) {
            deviceToken = existingOpt.get();
            deviceToken.setUserId(user.getUserId());
            deviceToken.setPlatform(platform);
            deviceToken.setActive(true);
        } else {
            deviceToken = new MobileDeviceToken(user.getUserId(), token, platform);
        }

        deviceTokenRepository.save(deviceToken);

        return ResponseEntity.ok(Map.of(
            "message", "Push token registered successfully",
            "token", token,
            "platform", platform
        ));
    }
}
