package com.fixzone.fixzon_backend.config;

import com.fixzone.fixzon_backend.model.SuperAdmin;
import com.fixzone.fixzon_backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Create Super Admin if not exists
        if (!userRepository.existsByRole("ROLE_SUPER_ADMIN")) {
            SuperAdmin admin = new SuperAdmin(
                    UUID.randomUUID(),
                    "Main Super Admin",
                    "admin@fixzone.com",
                    "+1-000-000-0000",
                    passwordEncoder.encode("Admin123!"),
                    "ROLE_SUPER_ADMIN",
                    true,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    "system",
                    LocalDateTime.now(),
                    "system",
                    "ADM-001"
            );
            
            userRepository.save(admin);
            System.out.println(">>> Super Admin account created: admin@fixzone.com / Admin123!");
        }
    }
}
