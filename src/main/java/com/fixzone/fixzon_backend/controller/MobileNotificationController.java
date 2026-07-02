package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.MobileDeviceTokenRequest;
import com.fixzone.fixzon_backend.service.MobileNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mobile-notifications")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class MobileNotificationController {

    private final MobileNotificationService mobileNotificationService;

    public MobileNotificationController(MobileNotificationService mobileNotificationService) {
        this.mobileNotificationService = mobileNotificationService;
    }

    @PostMapping("/tokens")
    public ResponseEntity<Void> registerToken(@RequestBody MobileDeviceTokenRequest request, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        String email = (String) authentication.getPrincipal();
        mobileNotificationService.registerToken(email, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tokens/deactivate")
    public ResponseEntity<Void> deactivateToken(@RequestBody MobileDeviceTokenRequest request, Authentication authentication) {
        // We only require the token string to deactivate it, but taking the full request is fine.
        mobileNotificationService.deactivateToken(request.getToken());
        return ResponseEntity.ok().build();
    }
}
