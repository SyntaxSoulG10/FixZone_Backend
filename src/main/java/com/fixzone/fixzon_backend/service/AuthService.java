package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.AuthRequestDTO;
import com.fixzone.fixzon_backend.DTO.AuthResponseDTO;
import com.fixzone.fixzon_backend.model.User;
import com.fixzone.fixzon_backend.repository.AuthRepository;
import com.fixzone.fixzon_backend.config.JwtUtil;
import com.fixzone.fixzon_backend.DTO.RegisterCustomerDTO;
import com.fixzone.fixzon_backend.DTO.RegisterOwnerDTO;
import com.fixzone.fixzon_backend.config.AppConstants;
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

    // Repository for user authentication data access
    @Autowired
    private AuthRepository authRepository;

    // Repository for customer-specific operations
    @Autowired
    private CustomerRepository customerRepository;

    // Repository for service owner operations
    @Autowired
    private OwnerRepository ownerRepository;

    // Password encoder for securing passwords (BCrypt)
    @Autowired
    private PasswordEncoder passwordEncoder;

    // JWT utility for token generation and validation
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Authenticates a user with email and password
     * @param request Contains email and password for authentication
     * @return AuthResponseDTO with JWT token and user details
     * @throws RuntimeException if email not found or password is invalid
     */
    public AuthResponseDTO login(AuthRequestDTO request) {
        // Step 1: Find user by email - throws exception if not found
        User user = authRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // Step 2: Verify password using BCrypt - compares hashes instead of plain text
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        // Step 3: Update last login timestamp for audit trail
        user.setLastLoginAt(LocalDateTime.now());
        authRepository.save(user);

        // Step 4: Generate JWT token with user claims (email, role, userId)
        String token = jwtUtil.generateToken(user);

        // Step 5: Return response with token and user metadata
        return new AuthResponseDTO(
                token,
                user.getUserId(),
                user.getEmail(),
                user.getRole(),
                user.getFullName()
        );
    }

    /**
     * Registers a new customer (vehicle owner)
     * @param request Contains fullName, email, and password
     * @return AuthResponseDTO with JWT token for the new customer
     * @throws RuntimeException if email already exists
     */
    public AuthResponseDTO registerCustomer(RegisterCustomerDTO request) {
        // Step 1: Check if email already exists to prevent duplicates
        if (authRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email is already taken");
        }

        // Step 2: Create new Customer entity
        Customer customer = new Customer();
        // Generate unique user ID using UUID
        customer.setUserId(UUID.randomUUID());
        customer.setFullName(request.getFullName());
        customer.setEmail(request.getEmail());
        
        // Step 3: Hash password using BCrypt - never store plain text passwords
        customer.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        
        // Step 4: Set initial customer properties
        customer.setRole(Role.ROLE_CUSTOMER.name()); // Set role as ROLE_CUSTOMER
        customer.setEmailVerified(false); // Email not yet verified
        customer.setStatus(AppConstants.STATUS_ACTIVE); // Set status to active
        // Generate unique customer code with timestamp for tracking
        customer.setCustomerCode(AppConstants.CUSTOMER_PREFIX + System.currentTimeMillis());

        // Step 5: Save customer to database
        customerRepository.save(customer);

        // Step 6: Generate JWT token for immediate login after registration
        String token = jwtUtil.generateToken(customer);

        // Step 7: Return response with token and customer details
        return new AuthResponseDTO(
                token,
                customer.getUserId(),
                customer.getEmail(),
                customer.getRole(),
                customer.getFullName()
        );
    }

    /**
     * Registers a new service center owner (company owner)
     * @param request Contains fullName, email, password, companyName, and companyNumber
     * @return AuthResponseDTO with JWT token for the new owner
     * @throws RuntimeException if email already exists
     */
    public AuthResponseDTO registerOwner(RegisterOwnerDTO request) {
        // Step 1: Check if email already exists to prevent duplicates
        if (authRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email is already taken");
        }

        // Step 2: Create new Owner entity
        Owner owner = new Owner();
        // Generate unique user ID using UUID
        owner.setUserId(UUID.randomUUID());
        owner.setFullName(request.getFullName());
        owner.setEmail(request.getEmail());
        
        // Step 3: Hash password using BCrypt - never store plain text passwords
        owner.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        
        // Step 4: Set initial owner properties
        owner.setRole(Role.ROLE_COMPANY_OWNER.name()); // Set role as ROLE_COMPANY_OWNER
        owner.setEmailVerified(false); // Email not yet verified
        owner.setStatus(AppConstants.STATUS_ACTIVE); // Set status to active
        // Generate unique owner code with timestamp for tracking
        owner.setOwnerCode(AppConstants.OWNER_PREFIX + System.currentTimeMillis());
        
        // Step 5: Set company-specific information
        owner.setCompanyName(request.getCompanyName()); // Business name
        owner.setCompanyNumber(request.getCompanyNumber()); // Business phone or registration number

        // Step 6: Save owner to database
        ownerRepository.save(owner);

        // Step 7: Generate JWT token for immediate login after registration
        String token = jwtUtil.generateToken(owner);

        // Step 8: Return response with token and owner details
        return new AuthResponseDTO(
                token,
                owner.getUserId(),
                owner.getEmail(),
                owner.getRole(),
                owner.getFullName()
        );
    }
}
