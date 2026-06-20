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
import com.fixzone.fixzon_backend.repository.SuperAdminRepository;
import com.fixzone.fixzon_backend.service.NotificationService;
import com.fixzone.fixzon_backend.model.SuperAdmin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@Service
@Slf4j
public class AuthService {

    private final AuthRepository authRepository;
    private final CustomerRepository customerRepository;
    private final OwnerRepository ownerRepository;
    private final SuperAdminRepository superAdminRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(AuthRepository authRepository,
                       CustomerRepository customerRepository,
                       OwnerRepository ownerRepository,
                       SuperAdminRepository superAdminRepository,
                       NotificationService notificationService,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.authRepository = authRepository;
        this.customerRepository = customerRepository;
        this.ownerRepository = ownerRepository;
        this.superAdminRepository = superAdminRepository;
        this.notificationService = notificationService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponseDTO login(AuthRequestDTO request) {
        log.info("Attempting login for email: {}", request.getEmail());
        
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            log.error("Login failed: Invalid password format for email {}", request.getEmail());
            throw new RuntimeException("Invalid email or password");
        }

        User user = authRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.error("Login failed: User not found for email {}", request.getEmail());
                    return new RuntimeException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.error("Login failed: Password mismatch for email {}", request.getEmail());
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
                user.getFullName(),
                user.getProfilePictureUrl(),
                user.getPhone()
        );
    }

    public AuthResponseDTO registerCustomer(RegisterCustomerDTO request) {
        log.info("Attempting to register customer with email: {}", request.getEmail());

        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        if (request.getEmail() == null || !request.getEmail().matches(emailRegex)) {
            log.error("Registration failed: Invalid email format: {}", request.getEmail());
            throw new RuntimeException("Invalid email format");
        }

        if (request.getPassword() == null || request.getPassword().length() < 8) {
            log.error("Registration failed: Password too short for email: {}", request.getEmail());
            throw new RuntimeException("Password must be at least 8 characters");
        }

        if (authRepository.findByEmail(request.getEmail()).isPresent()) {
            log.error("Registration failed: Email {} already taken", request.getEmail());
            throw new RuntimeException("Email is already taken");
        }

        // Sri Lankan mobile number validation
        // Expected formats: +947XXXXXXXX, 07XXXXXXXX, 7XXXXXXXX
        String phone = request.getPhone();
        if (phone != null && !phone.isEmpty()) {
            // Remove spaces for validation
            String cleanedPhone = phone.replace(" ", "");
            String regex = "^(\\+94|0)?7[0-9]{8}$";
            if (!cleanedPhone.matches(regex)) {
                log.error("Registration failed: Invalid Sri Lankan phone number format: {}", phone);
                throw new RuntimeException("Invalid Sri Lankan mobile number format");
            }
        }

        Customer customer = new Customer();
        customer.setUserId(UUID.randomUUID());
        customer.setFullName(request.getFullName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        customer.setRole(Role.ROLE_CUSTOMER.name());
        customer.setEmailVerified(false);
        customer.setStatus(AppConstants.STATUS_ACTIVE);
        customer.setCustomerCode(AppConstants.CUSTOMER_PREFIX + System.currentTimeMillis());

        Customer savedCustomer = customerRepository.save(customer);

        // Safe notification trigger
        triggerSignupNotifications(savedCustomer, "Customer");

        String token = jwtUtil.generateToken(customer);

        return new AuthResponseDTO(
                token,
                customer.getUserId(),
                customer.getEmail(),
                customer.getRole(),
                customer.getFullName(),
                customer.getProfilePictureUrl(),
                customer.getPhone()
        );
    }

    public AuthResponseDTO registerOwner(RegisterOwnerDTO request) {
        log.info("Attempting to register owner with email: {}", request.getEmail());

        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        if (request.getEmail() == null || !request.getEmail().matches(emailRegex)) {
            log.error("Registration failed: Invalid email format: {}", request.getEmail());
            throw new RuntimeException("Invalid email format");
        }

        if (request.getPassword() == null || request.getPassword().length() < 8) {
            log.error("Registration failed: Password too short for email: {}", request.getEmail());
            throw new RuntimeException("Password must be at least 8 characters");
        }

        if (authRepository.findByEmail(request.getEmail()).isPresent()) {
            log.error("Registration failed: Email {} already taken", request.getEmail());
            throw new RuntimeException("Email is already taken");
        }

        // Sri Lankan mobile number validation
       /*  String phone = request.getPhone();
        if (phone != null && !phone.isEmpty()) {
            String cleanedPhone = phone.replace(" ", "");
            String regex = "^(\\+94|0)?7[0-9]{8}$";
            if (!cleanedPhone.matches(regex)) {
                log.error("Registration failed: Invalid Sri Lankan phone number format: {}", phone);
                throw new RuntimeException("Invalid Sri Lankan mobile number format");
            }
        }*/

        Owner owner = new Owner();
        owner.setUserId(UUID.randomUUID());
        owner.setFullName(request.getFullName());
        owner.setEmail(request.getEmail());
        owner.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        owner.setRole(Role.ROLE_COMPANY_OWNER.name());
        owner.setEmailVerified(false);
        owner.setStatus(AppConstants.STATUS_ACTIVE);
        owner.setOwnerCode(AppConstants.OWNER_PREFIX + System.currentTimeMillis());
        owner.setCompanyName(request.getCompanyName());
        owner.setCompanyNumber(request.getCompanyNumber());

        Owner savedOwner = ownerRepository.save(owner);

        // Safe notification trigger
        triggerSignupNotifications(savedOwner, "Owner");

        String token = jwtUtil.generateToken(owner);

        return new AuthResponseDTO(
                token,
                owner.getUserId(),
                owner.getEmail(),
                owner.getRole(),
                owner.getFullName(),
                owner.getProfilePictureUrl(),
                owner.getPhone()
        );
    }

    private void triggerSignupNotifications(User user, String roleLabel) {
        try {
            String dashboardUrl = roleLabel.equalsIgnoreCase("Owner") ? "/dashboard/company-owner" : "/dashboard/customer";
            notificationService.createNotificationSafe(user, "Welcome to FixZone!", 
                "Hi " + user.getFullName() + ", welcome to FixZone! Your " + roleLabel.toLowerCase() + " account has been registered successfully.", 
                "SUCCESS", dashboardUrl);

            List<SuperAdmin> admins = superAdminRepository.findAll();
            notificationService.broadcastNotificationSafe(admins, "New User Registration", 
                "A new " + roleLabel.toLowerCase() + " has registered: " + user.getFullName() + " (" + user.getEmail() + ").", 
                "INFO", null);
        } catch (Exception e) {
            System.err.println("Error triggering signup notifications: " + e.getMessage());
        }
    }
}
