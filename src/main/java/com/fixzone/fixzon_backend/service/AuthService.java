package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.AuthRequestDTO;
import com.fixzone.fixzon_backend.DTO.AuthResponseDTO;
import com.fixzone.fixzon_backend.model.User;
import com.fixzone.fixzon_backend.repository.AuthRepository;
import com.fixzone.fixzon_backend.config.JwtUtil;
import com.fixzone.fixzon_backend.DTO.RegisterCustomerDTO;
import com.fixzone.fixzon_backend.DTO.RegisterOwnerDTO;
import com.fixzone.fixzon_backend.enums.Role;
import com.fixzone.fixzon_backend.model.Customer;
import com.fixzone.fixzon_backend.model.Owner;
import com.fixzone.fixzon_backend.repository.CustomerRepository;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public AuthResponseDTO login(AuthRequestDTO request) {
        User user = authRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        user.setLastLoginAt(LocalDateTime.now());
        authRepository.save(user);

        String token = jwtUtil.generateToken(user);

        return new AuthResponseDTO(
                token,
                user.getUserId(),
                user.getEmail(),
                user.getRole(),
                user.getFullName()
        );
    }

    public AuthResponseDTO registerCustomer(RegisterCustomerDTO request) {
        if (authRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email is already taken");
        }

        Customer customer = new Customer();
        customer.setUserId(UUID.randomUUID());
        customer.setFullName(request.getFullName());
        customer.setEmail(request.getEmail());
        customer.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        customer.setRole(Role.ROLE_CUSTOMER.name());
        customer.setEmailVerified(false);
        customer.setStatus("Active");
        customer.setCustomerCode("CUST-" + System.currentTimeMillis());

        customerRepository.save(customer);

        String token = jwtUtil.generateToken(customer);

        return new AuthResponseDTO(
                token,
                customer.getUserId(),
                customer.getEmail(),
                customer.getRole(),
                customer.getFullName()
        );
    }

    public AuthResponseDTO registerOwner(RegisterOwnerDTO request) {
        if (authRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email is already taken");
        }

        Owner owner = new Owner();
        owner.setUserId(UUID.randomUUID());
        owner.setFullName(request.getFullName());
        owner.setEmail(request.getEmail());
        owner.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        owner.setRole(Role.ROLE_COMPANY_OWNER.name());
        owner.setEmailVerified(false);
        owner.setStatus("Active");
        owner.setOwnerCode("OWN-" + System.currentTimeMillis());
        owner.setCompanyName(request.getCompanyName());
        owner.setCompanyNumber(request.getCompanyNumber());

        ownerRepository.save(owner);

        String token = jwtUtil.generateToken(owner);

        return new AuthResponseDTO(
                token,
                owner.getUserId(),
                owner.getEmail(),
                owner.getRole(),
                owner.getFullName()
        );
    }
}
