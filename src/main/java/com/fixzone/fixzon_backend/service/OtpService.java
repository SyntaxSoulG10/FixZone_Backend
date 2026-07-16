package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.model.VerificationToken;
import com.fixzone.fixzon_backend.repository.VerificationTokenRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class OtpService {

    private final VerificationTokenRepository tokenRepository;
    private final EmailService emailService;

    public OtpService(VerificationTokenRepository tokenRepository, EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    public void generateAndSendOtp(String email, String fullName) {
        String otp = String.format("%05d", new Random().nextInt(100000));

        VerificationToken token = new VerificationToken();
        token.setEmail(email);
        token.setOtpCode(otp);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(10));
        token.setUsed(false);
        tokenRepository.save(token);

        emailService.sendVerificationOtpEmail(email, fullName, otp);
    }

    public boolean verifyOtp(String email, String otpCode) {
        System.out.println("DEBUG: Finding tokens for email: [" + email + "] and code: [" + otpCode + "]");
        java.util.List<VerificationToken> allTokens = tokenRepository.findAll();
        for (VerificationToken t : allTokens) {
            if (t.getEmail().equals(email)) {
                System.out.println("DEBUG: Found token for this email: Code=[" + t.getOtpCode() + "], Used=" + t.isUsed() + ", Expiry=" + t.getExpiryDate());
            }
        }

        Optional<VerificationToken> tokenOpt = tokenRepository.findByEmailAndOtpCode(email, otpCode);
        if (tokenOpt.isPresent()) {
            VerificationToken token = tokenOpt.get();
            System.out.println("DEBUG: Token found. Used: " + token.isUsed() + ", Expiry: " + token.getExpiryDate() + ", Now: " + LocalDateTime.now());
            if (!token.isUsed() && token.getExpiryDate().isAfter(LocalDateTime.now())) {
                token.setUsed(true);
                tokenRepository.save(token);
                return true;
            } else {
                System.out.println("DEBUG: Token rejected. isUsed=" + token.isUsed() + ", isExpired=" + !token.getExpiryDate().isAfter(LocalDateTime.now()));
            }
        } else {
            System.out.println("DEBUG: No token found matching exact email and otpCode.");
        }
        return false;
    }
}
