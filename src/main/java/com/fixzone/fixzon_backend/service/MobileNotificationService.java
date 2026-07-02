package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.MobileDeviceTokenRequest;
import com.fixzone.fixzon_backend.model.MobileDeviceToken;
import com.fixzone.fixzon_backend.model.User;
import com.fixzone.fixzon_backend.repository.MobileDeviceTokenRepository;
import com.fixzone.fixzon_backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class MobileNotificationService {

    private final MobileDeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    public MobileNotificationService(MobileDeviceTokenRepository deviceTokenRepository, UserRepository userRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.userRepository = userRepository;
        this.restTemplate = new RestTemplate();
    }

    @Transactional
    public void registerToken(String email, MobileDeviceTokenRequest request) {
        if (request.getToken() == null || request.getToken().trim().isEmpty()) {
            return;
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        Optional<MobileDeviceToken> existingTokenOpt = deviceTokenRepository.findByToken(request.getToken());

        if (existingTokenOpt.isPresent()) {
            MobileDeviceToken tokenEntity = existingTokenOpt.get();
            // If the token is registered to a different user, reassign it
            if (!tokenEntity.getUser().getUserId().equals(user.getUserId())) {
                tokenEntity.setUser(user);
            }
            tokenEntity.setActive(true);
            tokenEntity.setLastSeen(LocalDateTime.now());
            deviceTokenRepository.save(tokenEntity);
        } else {
            MobileDeviceToken newToken = new MobileDeviceToken();
            newToken.setUser(user);
            newToken.setToken(request.getToken());
            newToken.setPlatform(request.getPlatform());
            newToken.setDeviceName(request.getDeviceName());
            newToken.setActive(true);
            deviceTokenRepository.save(newToken);
        }
    }

    @Transactional
    public void deactivateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }
        deviceTokenRepository.findByToken(token).ifPresent(tokenEntity -> {
            tokenEntity.setActive(false);
            deviceTokenRepository.save(tokenEntity);
        });
    }

    // Next step will involve implementing Expo Push Notification delivery
}
