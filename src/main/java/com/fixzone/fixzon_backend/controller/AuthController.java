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
}
