package com.fixzone.fixzon_backend.config;

import com.fixzone.fixzon_backend.model.*;
import com.fixzone.fixzon_backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ServiceCenterRepository serviceCenterRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public DataInitializer(ServiceCenterRepository serviceCenterRepository,
            ServicePackageRepository servicePackageRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate) {
        this.serviceCenterRepository = serviceCenterRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println(">>> RESTORING TEAM WORK DATA <<<");

        // 1. Repair Schema First
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE payments ALTER COLUMN booking_id TYPE bigint USING (booking_id::text::bigint)");
        } catch (Exception e) {
        }

        // 2. Restore Original Users
        User admin = ensureUser(UUID.fromString("11111111-1111-1111-1111-111111111111"), "admin@fixzone.com",
                "Main Super Admin", "ROLE_SUPER_ADMIN", "Admin123!");
        User owner1 = ensureUser(UUID.fromString("22222222-2222-2222-2222-222222222222"), "owner1@fixzone.com",
                "Elite Owner", "ROLE_COMPANY_OWNER", "Owner123!");
        User owner2 = ensureUser(UUID.fromString("33333333-3333-3333-3333-333333333333"), "owner2@fixzone.com",
                "Zenith Owner", "ROLE_COMPANY_OWNER", "Owner123!");
        User charlie = ensureUser(UUID.fromString("00000000-0000-0000-0000-000000000001"), "charlie@example.com",
                "Charlie Customer", "ROLE_CUSTOMER", "charlie123");

        // 3. Restore Original Service Centers
        if (serviceCenterRepository.count() == 0) {
            ServiceCenter sc1 = createCenter(UUID.fromString("a1e11111-1111-1111-1111-111111111111"), "Elite Motors",
                    "12 High Level Rd, Nugegoda", "+1-555-0101", owner1);
            ServiceCenter sc2 = createCenter(UUID.fromString("b2e22222-2222-2222-2222-222222222222"),
                    "Zenith Performance", "45 Station Rd, Bambalapitiya", "+1-555-0102", owner2);
            ServiceCenter sc3 = createCenter(UUID.fromString("c3e33333-3333-3333-3333-333333333333"),
                    "City Tire & Lube", "78 Peradeniya Rd, Kandy", "+1-555-0103", owner1);

            // 4. Restore Original Packages
            createPackage(sc1, "Full Maintenance", "Oil Change, Filter, Brake Check",
                    "Comprehensive vehicle inspection.", "9500.00", 120);
            createPackage(sc1, "Basic Service", "Oil Change", "Quick check-up.", "4500.00", 45);
            createPackage(sc1, "AC Refill & Service", "Gas recharge", "Keep it cool.", "5500.00", 60);

            createPackage(sc2, "Performance Tune-up", "ECU Remap", "Maximize output.", "450.00", 180);
            createPackage(sc2, "Premium Detailing", "Wax & Polish", "Showroom shine.", "15000.00", 240);

            createPackage(sc3, "Tire Rotation & Balance", "4 Wheels", "Even wear.", "2500.00", 40);
            createPackage(sc3, "Wheel Alignment", "Precision Alignment", "Smooth steering.", "3500.00", 50);
            createPackage(sc3, "Hybrid Battery Health", "Scan & Report", "Efficiency check.", "4000.00", 45);

            System.out.println(">>> ORIGINAL TEAM DATA RESTORED SUCCESSFULLY <<<");
        }

        System.out.println(">>> ENVIRONMENT READY <<<");
    }

    private User ensureUser(UUID id, String email, String name, String role, String pass) {
        return userRepository.findById(id).orElseGet(() -> {
            User u = new User();
            u.setUserId(id);
            u.setEmail(email);
            u.setFullName(name);
            u.setRole(role);
            u.setPasswordHash(passwordEncoder.encode(pass));
            u.setStatus("Active");
            return userRepository.save(u);
        });
    }

    private ServiceCenter createCenter(UUID id, String name, String addr, String phone, User owner) {
        ServiceCenter sc = new ServiceCenter();
        sc.setCenterId(id);
        sc.setName(name);
        sc.setAddress(addr);
        sc.setContactPhone(phone);
        sc.setOwner(owner);
        sc.setIsActive(true);
        sc.setStatus("APPROVED");
        sc.setOpeningHours("08:00 - 18:00");
        return serviceCenterRepository.save(sc);
    }

    private void createPackage(ServiceCenter sc, String name, String type, String desc, String price, int duration) {
        ServicePackage p = new ServicePackage();
        p.setPackageId(UUID.randomUUID());
        p.setServiceCenter(sc);
        p.setName(name);
        p.setType(type);
        p.setDescription(desc);
        p.setBasePrice(new BigDecimal(price));
        p.setEstimatedDurationMins(duration);
        p.setIsActive(true);
        servicePackageRepository.save(p);
    }
}
