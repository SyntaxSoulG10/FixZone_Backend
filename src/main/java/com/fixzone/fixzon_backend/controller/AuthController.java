package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.AuthRequestDTO;
import com.fixzone.fixzon_backend.DTO.AuthResponseDTO;
import com.fixzone.fixzon_backend.DTO.RegisterCustomerDTO;
import com.fixzone.fixzon_backend.DTO.RegisterOwnerDTO;
import com.fixzone.fixzon_backend.DTO.VerifyOtpRequestDTO;
import com.fixzone.fixzon_backend.DTO.ResendOtpRequestDTO;
import com.fixzone.fixzon_backend.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@Tag(name = "Authentication", description = "Endpoints for User Login, Registration, OTP Verification, and Password Reset")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final com.fixzone.fixzon_backend.service.ImageKitService imageKitService;

    public AuthController(AuthService authService, com.fixzone.fixzon_backend.service.ImageKitService imageKitService) {
        this.authService = authService;
        this.imageKitService = imageKitService;
    }

    @Operation(summary = "Get ImageKit upload authentication parameters")
    @GetMapping("/imagekit-auth")
    public ResponseEntity<?> getImageKitAuth() {
        return ResponseEntity.ok(imageKitService.getAuthenticationParameters());
    }

    @Operation(summary = "Authenticate user and receive JWT token")
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody AuthRequestDTO authRequestDTO) {
        AuthResponseDTO response = authService.login(authRequestDTO);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Register a new Customer account")
    @PostMapping("/register/customer")
    public ResponseEntity<AuthResponseDTO> registerCustomer(@jakarta.validation.Valid @RequestBody RegisterCustomerDTO request) {
        AuthResponseDTO response = authService.registerCustomer(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Register a new Service Center Owner account")
    @PostMapping("/register/owner")
    public ResponseEntity<AuthResponseDTO> registerOwner(@jakarta.validation.Valid @RequestBody RegisterOwnerDTO request) {
        AuthResponseDTO response = authService.registerOwner(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody VerifyOtpRequestDTO request) {
        System.out.println("VERIFY OTP ATTEMPT: Email=" + request.getEmail() + ", OTP=" + request.getOtpCode());
        boolean isVerified = authService.verifyEmail(request.getEmail(), request.getOtpCode());
        System.out.println("VERIFY RESULT: " + isVerified);
        if (isVerified) {
            return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
        } else {
            // Debugging output to see what is failing
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired verification code (Email: " + request.getEmail() + ", OTP received: " + request.getOtpCode() + ")"));
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody ResendOtpRequestDTO request) {
        try {
            authService.resendOtp(request.getEmail());
            return ResponseEntity.ok(Map.of("message", "Verification code resent successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/validate-email")
    public ResponseEntity<?> validateEmail(@RequestParam String email) {
        boolean isValid = com.fixzone.fixzon_backend.util.EmailValidator.isValidRealEmail(email);
        if (isValid) {
            return ResponseEntity.ok(Map.of("valid", true, "message", "Email domain has active mail servers."));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                "valid", false, 
                "message", "Invalid email: The email domain does not have an active mail server (MX record) or does not exist."
            ));
        }
    }

    @PostMapping("/activate-manager")
    public ResponseEntity<?> activateManager(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            String password = request.get("password");
            if (token == null || password == null) {
                throw new IllegalArgumentException("Token and password are required");
            }
            authService.activateAccount(token, password);
            return ResponseEntity.ok(Map.of("message", "Account activated successfully! You can now log in."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
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
            response.put("message", e.getMessage());
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

    @PostMapping("/verify-reset-otp")
    public ResponseEntity<java.util.Map<String, String>> verifyResetOtp(@RequestBody java.util.Map<String, String> request) {
        try {
            String email = request.get("email");
            String token = request.get("token");
            if (token == null || token.trim().isEmpty()) {
                throw new IllegalArgumentException("Verification code is required");
            }
            authService.validateResetToken(email, token);
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("message", "Verification code is valid");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
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
