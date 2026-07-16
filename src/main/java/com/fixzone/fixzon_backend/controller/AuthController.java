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
}
