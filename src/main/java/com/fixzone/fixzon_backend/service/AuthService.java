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
import com.fixzone.fixzon_backend.model.SuperAdmin;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@Service
@Transactional
public class AuthService {

    private final AuthRepository authRepository;
    private final CustomerRepository customerRepository;
    private final OwnerRepository ownerRepository;
    private final SuperAdminRepository superAdminRepository;
    private final ServiceCenterRepository serviceCenterRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    @org.springframework.beans.factory.annotation.Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public AuthService(AuthRepository authRepository,
            CustomerRepository customerRepository,
            OwnerRepository ownerRepository,
            SuperAdminRepository superAdminRepository,
            ServiceCenterRepository serviceCenterRepository,
            NotificationService notificationService,
            PasswordEncoder passwordEncoder,
            OtpService otpService,
            JwtUtil jwtUtil,
            EmailService emailService) {
        this.authRepository = authRepository;
        this.customerRepository = customerRepository;
        this.ownerRepository = ownerRepository;
        this.superAdminRepository = superAdminRepository;
        this.serviceCenterRepository = serviceCenterRepository;
        this.notificationService = notificationService;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
    }

    public void forgotPassword(String email) {
        User user = authRepository.findByEmail(email)
                .orElseThrow(
                        () -> new IllegalArgumentException("If the email is registered, a reset link will be sent."));

        String token = String.format("%05d", new java.util.Random().nextInt(100000));
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        authRepository.save(user);

        String cleanFrontendUrl = frontendUrl != null
                ? frontendUrl.trim().replaceAll("^[\"']|[\"']$", "").replaceAll("/+$", "")
                : "http://localhost:3000";
        if (!cleanFrontendUrl.startsWith("http://") && !cleanFrontendUrl.startsWith("https://")) {
            cleanFrontendUrl = "https://" + cleanFrontendUrl;
        }

        String resetLink = cleanFrontendUrl + "/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink, token);

        notificationService.createNotificationSafe(user, "Password Reset Requested",
                "A password recovery verification code was requested for your account.", "INFO", null);
    }

    public void validateResetToken(String email, String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Verification code is required");
        }
        User user = authRepository.findByResetToken(token.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification code"));

        if (email != null && !email.isBlank() && !user.getEmail().equalsIgnoreCase(email.trim())) {
            throw new IllegalArgumentException("Verification code does not match this email");
        }

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Verification code has expired. Please request a new code.");
        }
    }

    private void validatePasswordComplexity(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least 1 uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain at least 1 lowercase letter");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("Password must contain at least 1 number");
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new IllegalArgumentException("Password must contain at least 1 special character");
        }
    }

    public void resetPassword(String token, String newPassword) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Reset token is required");
        }
        String cleanToken = token.trim();
        User user = authRepository.findByResetToken(cleanToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Reset token has expired. Please request a new code.");
        }

        validatePasswordComplexity(newPassword);

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from your current password");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        authRepository.save(user);

        notificationService.createNotificationSafe(user, "Password Changed Successfully",
                "Your password has been reset successfully. If you did not perform this action, please contact support immediately.",
                "WARNING", null);
    }

    public AuthResponseDTO login(AuthRequestDTO request) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String rawEmail = request.getEmail().trim();
        String cleanEmail = rawEmail.toLowerCase();

        User user = authRepository.findByEmailIgnoreCase(cleanEmail)
                .or(() -> authRepository.findByEmail(rawEmail))
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        String rawPassword = request.getPassword();
        boolean matches = passwordEncoder.matches(rawPassword, user.getPasswordHash())
                || passwordEncoder.matches(rawPassword.trim(), user.getPasswordHash());

        if (!matches) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        if ("Suspended".equalsIgnoreCase(user.getStatus())) {
            String reason = user.getSuspensionReason() != null ? user.getSuspensionReason() : "Contact support.";
            throw new IllegalArgumentException(
                    "Your account has been suspended by an administrator. Reason: " + reason);
        }

        // Automatically activate account on first successful login
        if ("INVITED".equalsIgnoreCase(user.getStatus()) || "Pending".equalsIgnoreCase(user.getStatus())) {
            user.setStatus(AppConstants.STATUS_ACTIVE);
            user.setEmailVerified(true);
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
                user.getPhone(),
                user.getEmailVerified() != null ? user.getEmailVerified() : false);
    }

    public AuthResponseDTO registerCustomer(RegisterCustomerDTO request) {
        if (authRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already taken");
        }

        validatePasswordComplexity(request.getPassword());

        Customer customer = new Customer();
        customer.setUserId(UUID.randomUUID());
        customer.setFullName(request.getFullName());
        customer.setEmail(request.getEmail());
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            customer.setPhone(request.getPhone());
        }
        customer.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        customer.setRole(Role.ROLE_CUSTOMER.name());
        customer.setEmailVerified(false);
        customer.setStatus(AppConstants.STATUS_ACTIVE);
        customer.setCustomerCode(AppConstants.CUSTOMER_PREFIX + System.currentTimeMillis());

        Customer savedCustomer = customerRepository.save(customer);

        // Safe notification trigger
        triggerSignupNotifications(savedCustomer, "Customer");

        // Send OTP
        otpService.generateAndSendOtp(savedCustomer.getEmail(), savedCustomer.getFullName());

        String token = jwtUtil.generateToken(customer);

        return new AuthResponseDTO(
                token,
                customer.getUserId(),
                customer.getEmail(),
                customer.getRole(),
                customer.getFullName(),
                customer.getProfilePictureUrl(),
                customer.getPhone(),
                customer.getEmailVerified() != null ? customer.getEmailVerified() : false);
    }

    @org.springframework.transaction.annotation.Transactional
    public AuthResponseDTO registerOwner(RegisterOwnerDTO request) {
        if (authRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already taken");
        }

        validatePasswordComplexity(request.getPassword());

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
        owner.setSubscriptionStatus("TRIAL_ACTIVE");
        owner.setTrialEndsAt(LocalDateTime.now().plusDays(30));

        Owner savedOwner = ownerRepository.save(owner);

        ServiceCenter serviceCenter = new ServiceCenter();
        serviceCenter.setCenterId(UUID.randomUUID());
        serviceCenter.setOwner(savedOwner);
        serviceCenter.setName(request.getCompanyName() != null ? request.getCompanyName() : "HQ");
        serviceCenter.setContactPhone(request.getCompanyNumber());
        serviceCenter.setStatus("PENDING");
        serviceCenter.setIsActive(false);
        serviceCenter.setBusinessRegUrl(request.getBusinessRegUrl());
        serviceCenter.setTaxIdUrl(request.getTaxIdUrl());
        serviceCenter.setNicUrl(request.getNicUrl());
        serviceCenterRepository.save(serviceCenter);

        // Safe notification trigger
        triggerSignupNotifications(savedOwner, "Owner");

        // Send OTP
        otpService.generateAndSendOtp(savedOwner.getEmail(), savedOwner.getFullName());

        String token = jwtUtil.generateToken(owner);

        return new AuthResponseDTO(
                token,
                owner.getUserId(),
                owner.getEmail(),
                owner.getRole(),
                owner.getFullName(),
                owner.getProfilePictureUrl(),
                owner.getPhone(),
                owner.getEmailVerified() != null ? owner.getEmailVerified() : false);
    }

    @org.springframework.transaction.annotation.Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        if (currentPassword == null || currentPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Current password is required");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("New password is required");
        }

        User user = authRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash()) || currentPassword.equals(newPassword)) {
            throw new IllegalArgumentException("New password must be different from your current password");
        }

        // Validate password complexity requirements
        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (!newPassword.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least 1 uppercase letter");
        }
        if (!newPassword.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain at least 1 lowercase letter");
        }
        if (!newPassword.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("Password must contain at least 1 number");
        }
        if (!newPassword.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new IllegalArgumentException("Password must contain at least 1 special character");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        authRepository.save(user);

        notificationService.createNotificationSafe(user, "Password Updated",
                "Your password was updated successfully from account settings.", "INFO", null);
    }

    private void triggerSignupNotifications(User user, String roleLabel) {
        try {
            String dashboardUrl = roleLabel.equalsIgnoreCase("Owner") ? "/dashboard/company-owner"
                    : "/dashboard/customer";
            notificationService.createNotificationSafe(user, "Welcome to FixZone!",
                    "Hi " + user.getFullName() + ", welcome to FixZone! Your " + roleLabel.toLowerCase()
                            + " account has been registered successfully.",
                    "SUCCESS", dashboardUrl);

            List<SuperAdmin> admins = superAdminRepository.findAll();
            notificationService.broadcastNotificationSafe(admins, "New User Registration",
                    "A new " + roleLabel.toLowerCase() + " has registered: " + user.getFullName() + " ("
                            + user.getEmail() + ").",
                    "INFO", null);
        } catch (Exception e) {
            System.err.println("Error triggering signup notifications: " + e.getMessage());
        }
    }

    public boolean verifyEmail(String email, String otpCode) {
        if (otpService.verifyOtp(email, otpCode)) {
            User user = authRepository.findByEmail(email).orElse(null);
            if (user != null) {
                user.setEmailVerified(true);
                authRepository.save(user);
                return true;
            }
        }
        return false;
    }

    public void resendOtp(String email) {
        User user = authRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        otpService.generateAndSendOtp(user.getEmail(), user.getFullName());
    }

    public void activateAccount(String token, String newPassword) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Activation token is required");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        User user = authRepository.findByResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired invitation link"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "This invitation link has expired. Please request a new invitation from your company owner.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setStatus(AppConstants.STATUS_ACTIVE);
        user.setEmailVerified(true);
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        user.setUpdatedAt(LocalDateTime.now());
        authRepository.save(user);
    }
}
