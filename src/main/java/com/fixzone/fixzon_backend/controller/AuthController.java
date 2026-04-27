package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.AuthRequestDTO;
import com.fixzone.fixzon_backend.DTO.AuthResponseDTO;
import com.fixzone.fixzon_backend.DTO.ErrorResponseDTO;
import com.fixzone.fixzon_backend.DTO.RegisterCustomerDTO;
import com.fixzone.fixzon_backend.DTO.RegisterOwnerDTO;
import com.fixzone.fixzon_backend.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequestDTO authRequestDTO) {
        try {
            AuthResponseDTO response = authService.login(authRequestDTO);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Login error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ErrorResponseDTO(e.getMessage()));
        }
    }

    @PostMapping("/register/customer")
    public ResponseEntity<?> registerCustomer(@RequestBody RegisterCustomerDTO request) {
        try {
            AuthResponseDTO response = authService.registerCustomer(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Customer registration error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ErrorResponseDTO(e.getMessage()));
        }
    }

    @PostMapping("/register/owner")
    public ResponseEntity<?> registerOwner(@RequestBody RegisterOwnerDTO request) {
        try {
            AuthResponseDTO response = authService.registerOwner(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Owner registration error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ErrorResponseDTO(e.getMessage()));
        }
    }
}
