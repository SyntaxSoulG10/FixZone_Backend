package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.AuthRequestDTO;
import com.fixzone.fixzon_backend.DTO.AuthResponseDTO;
import com.fixzone.fixzon_backend.DTO.RegisterCustomerDTO;
import com.fixzone.fixzon_backend.DTO.RegisterOwnerDTO;
import com.fixzone.fixzon_backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequestDTO authRequestDTO) {
        try {
            AuthResponseDTO response = authService.login(authRequestDTO);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/register/customer")
    public ResponseEntity<AuthResponseDTO> registerCustomer(@RequestBody RegisterCustomerDTO request) {
        try {
            AuthResponseDTO response = authService.registerCustomer(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/register/owner")
    public ResponseEntity<AuthResponseDTO> registerOwner(@RequestBody RegisterOwnerDTO request) {
        try {
            AuthResponseDTO response = authService.registerOwner(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
