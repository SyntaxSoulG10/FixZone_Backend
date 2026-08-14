package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.AuthRequestDTO;
import com.fixzone.fixzon_backend.DTO.AuthResponseDTO;
import com.fixzone.fixzon_backend.DTO.RegisterCustomerDTO;
import com.fixzone.fixzon_backend.DTO.RegisterOwnerDTO;
import com.fixzone.fixzon_backend.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody AuthRequestDTO authRequestDTO) {
        AuthResponseDTO response = authService.login(authRequestDTO);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/customer")
    public ResponseEntity<AuthResponseDTO> registerCustomer(@RequestBody RegisterCustomerDTO request) {
        AuthResponseDTO response = authService.registerCustomer(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/owner")
    public ResponseEntity<AuthResponseDTO> registerOwner(@RequestBody RegisterOwnerDTO request) {
        AuthResponseDTO response = authService.registerOwner(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<java.util.Map<String, String>> changePassword(
            @jakarta.validation.Valid @RequestBody com.fixzone.fixzon_backend.DTO.ChangePasswordRequestDTO request) {
        try {
            String email = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            authService.changePassword(email, request.getCurrentPassword(), request.getNewPassword());
            
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("message", "Password changed successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<java.util.Map<String, String>> forgotPassword(@RequestBody java.util.Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.trim().isEmpty()) {
                throw new IllegalArgumentException("Email is required");
            }
            authService.forgotPassword(email);
            
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("message", "If the email is registered, a reset link will be sent.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("message", "If the email is registered, a reset link will be sent.");
            return ResponseEntity.ok(response); // Always return OK to prevent email enumeration
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<java.util.Map<String, String>> resetPassword(@RequestBody java.util.Map<String, String> request) {
        try {
            String token = request.get("token");
            String newPassword = request.get("newPassword");
            
            if (token == null || newPassword == null) {
                throw new IllegalArgumentException("Token and new password are required");
            }
            
            authService.resetPassword(token, newPassword);
            
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("message", "Password reset successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
