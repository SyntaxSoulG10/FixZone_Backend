package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByEmailAndOtpCode(String email, String otpCode);
    Optional<VerificationToken> findTopByEmailOrderByExpiryDateDesc(String email);
}
