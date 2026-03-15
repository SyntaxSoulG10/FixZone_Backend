package com.fixzone.fixzon_backend.config;

import com.fixzone.fixzon_backend.entity.User;
import com.fixzone.fixzon_backend.enums.Role;
import com.fixzone.fixzon_backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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
        if (!userRepository.existsByRole(Role.ROLE_SUPER_ADMIN)) {
            User admin = User.builder()
                    .name("Main Super Admin")
                    .email("admin@fixzone.com")
                    .password(passwordEncoder.encode("Admin123!"))
                    .role(Role.ROLE_SUPER_ADMIN)
                    .status("Active")
                    .build();
            
            userRepository.save(admin);
            System.out.println(">>> Super Admin account created: admin@fixzone.com / Admin123!");
        }
    }
}
