package com.fixzone.fixzon_backend.config;

import com.fixzone.fixzon_backend.model.*;
import com.fixzone.fixzon_backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ServiceCenterRepository serviceCenterRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final PasswordEncoder passwordEncoder;

    // Fixed IDs for consistency across branches
    private static final UUID CENTER_1_ID = UUID.fromString("c0000000-0000-0000-0000-000000000001");
    private static final UUID CENTER_2_ID = UUID.fromString("c0000000-0000-0000-0000-000000000002");
    private static final UUID CENTER_3_ID = UUID.fromString("c0000000-0000-0000-0000-000000000003");
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000010006");
    private static final UUID OWNER_1_ID = UUID.fromString("00000000-0000-0000-0000-000000010011");
    private static final UUID OWNER_2_ID = UUID.fromString("00000000-0000-0000-0000-000000010012");
    private static final UUID OWNER_3_ID = UUID.fromString("00000000-0000-0000-0000-000000010013");

    public DataInitializer(UserRepository userRepository, 
                           ServiceCenterRepository serviceCenterRepository,
                           ServicePackageRepository servicePackageRepository,
                           InvoiceRepository invoiceRepository,
                           PaymentRecordRepository paymentRecordRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.serviceCenterRepository = serviceCenterRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("--- [INITIALIZER] RUNNING DATA SEEDING ---");

        // 1. Super Admin
        if (!userRepository.existsByRole("ROLE_SUPER_ADMIN")) {
            SuperAdmin admin = new SuperAdmin(UUID.randomUUID(), "Main Super Admin", "admin@fixzone.com", 
                "+1-000-000-0000", passwordEncoder.encode("Admin123!"), "ROLE_SUPER_ADMIN", 
                true, LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "ADM-001");
            userRepository.save(admin);
            System.out.println(">>> Super Admin created.");
        }

        // 2. Owners & Customers
        List<User> newUsers = new ArrayList<>();
        if (!userRepository.existsByEmail("e.taylor@fixzone.com")) {
            newUsers.add(new Owner(OWNER_1_ID, "Elizabeth Taylor", "e.taylor@fixzone.com", "+12025550111", 
                passwordEncoder.encode("pass123"), "OWNER", true, LocalDateTime.now(), LocalDateTime.now(), 
                "system", LocalDateTime.now(), "system", "OWN-001", "Taylor Logistics", "contact@taylorlogs.com", "+15550111"));
        }
        if (!userRepository.existsByEmail("richard.moore@fixzone.com")) {
            newUsers.add(new Owner(OWNER_2_ID, "Richard Moore", "richard.moore@fixzone.com", "+12025550112", 
                passwordEncoder.encode("pass123"), "OWNER", true, LocalDateTime.now(), LocalDateTime.now(), 
                "system", LocalDateTime.now(), "system", "OWN-002", "Moore Repairs", "info@moore.com", "+15550112"));
        }
        if (!userRepository.existsByEmail("emily.s@fixzone.com")) {
            newUsers.add(new Owner(OWNER_3_ID, "Emily Sarah", "emily.s@fixzone.com", "+12025550113", 
                passwordEncoder.encode("pass123"), "OWNER", true, LocalDateTime.now(), LocalDateTime.now(), 
                "system", LocalDateTime.now(), "system", "OWN-003", "City Lube Group", "support@citylube.com", "+15550113"));
        }
        if (!userRepository.existsByEmail("david.t@example.com")) {
            newUsers.add(new Customer(CUSTOMER_ID, "David Thompson", "david.t@example.com", "+12025550106", 
                passwordEncoder.encode("pass123"), "CUSTOMER", true, LocalDateTime.now(), LocalDateTime.now(), 
                "system", LocalDateTime.now(), "system", "CUST-001", "EMAIL"));
        }
        
        if (!newUsers.isEmpty()) {
            userRepository.saveAll(newUsers);
            System.out.println(">>> New Owners/Customers added.");
        }
            
        // Centers
        if (!serviceCenterRepository.existsById(Objects.requireNonNull(CENTER_1_ID))) {
            Owner o1 = (Owner) userRepository.findById(Objects.requireNonNull(OWNER_1_ID)).orElse(null);
            serviceCenterRepository.save(new ServiceCenter(CENTER_1_ID, o1, "Taylor Express Maintenance", "456 Galle Road, Colombo", "+1-555-0101", "08:00 - 18:00", new BigDecimal("4.8"), true, LocalDateTime.now(), "system", LocalDateTime.now(), "system", new String[]{"Toyota", "Honda"}, "APPROVED", null));
        }
        if (!serviceCenterRepository.existsById(Objects.requireNonNull(CENTER_2_ID))) {
            Owner o2 = (Owner) userRepository.findById(Objects.requireNonNull(OWNER_2_ID)).orElse(null);
            serviceCenterRepository.save(new ServiceCenter(CENTER_2_ID, o2, "Moore Precision Repairs", "123 Main St, Colombo", "+1-555-0102", "09:00 - 19:00", new BigDecimal("4.5"), true, LocalDateTime.now(), "system", LocalDateTime.now(), "system", new String[]{"BMW", "Audi"}, "APPROVED", null));
        }
        if (!serviceCenterRepository.existsById(Objects.requireNonNull(CENTER_3_ID))) {
            Owner o3 = (Owner) userRepository.findById(Objects.requireNonNull(OWNER_3_ID)).orElse(null);
            serviceCenterRepository.save(new ServiceCenter(CENTER_3_ID, o3, "City Tire & Lube", "78 Peradeniya Rd, Kandy", "+1-555-0103", "07:00 - 17:00", new BigDecimal("4.2"), true, LocalDateTime.now(), "system", LocalDateTime.now(), "system", new String[]{"Nissan", "Mitsubishi"}, "APPROVED", null));
            System.out.println(">>> New Service Center (City Tire & Lube) added.");
        }

        // 3. Packages (Independent check)
        if (servicePackageRepository.count() < 10) {
            System.out.println(">>> Expanding Service Packages Data...");
            servicePackageRepository.deleteAll(); // Refresh with new expanded set
            
            ServiceCenter sc1 = serviceCenterRepository.findById(Objects.requireNonNull(CENTER_1_ID)).orElse(null);
            ServiceCenter sc2 = serviceCenterRepository.findById(Objects.requireNonNull(CENTER_2_ID)).orElse(null);
            ServiceCenter sc3 = serviceCenterRepository.findById(Objects.requireNonNull(CENTER_3_ID)).orElse(null);
            
            if (sc1 != null && sc2 != null && sc3 != null) {
                List<ServicePackage> pkgs = new ArrayList<>();
                // Center 1
                pkgs.add(new ServicePackage(UUID.randomUUID(), sc1, "Full Maintenance", "Oil Change, Filter, Brake Check", "Comprehensive vehicle inspection.", new BigDecimal("9500.00"), 120, true, LocalDateTime.now(), "system", LocalDateTime.now(), "system"));
                pkgs.add(new ServicePackage(UUID.randomUUID(), sc1, "Basic Service", "Oil Change", "Quick check-up.", new BigDecimal("4500.00"), 45, true, LocalDateTime.now(), "system", LocalDateTime.now(), "system"));
                pkgs.add(new ServicePackage(UUID.randomUUID(), sc1, "AC Refill & Service", "Gas recharge", "Keep it cool.", new BigDecimal("5500.00"), 60, true, LocalDateTime.now(), "system", LocalDateTime.now(), "system"));
                
                // Center 2
                pkgs.add(new ServicePackage(UUID.randomUUID(), sc2, "Performance Tune-up", "ECU Remap", "Maximize output.", new BigDecimal("450.00"), 180, true, LocalDateTime.now(), "system", LocalDateTime.now(), "system"));
                pkgs.add(new ServicePackage(UUID.randomUUID(), sc2, "Premium Detailing", "Wax & Polish", "Showroom shine.", new BigDecimal("15000.00"), 240, true, LocalDateTime.now(), "system", LocalDateTime.now(), "system"));
                
                // Center 3
                pkgs.add(new ServicePackage(UUID.randomUUID(), sc3, "Tire Rotation & Balance", "4 Wheels", "Even wear.", new BigDecimal("2500.00"), 40, true, LocalDateTime.now(), "system", LocalDateTime.now(), "system"));
                pkgs.add(new ServicePackage(UUID.randomUUID(), sc3, "Wheel Alignment", "Precision Alignment", "Smooth steering.", new BigDecimal("3500.00"), 50, true, LocalDateTime.now(), "system", LocalDateTime.now(), "system"));
                pkgs.add(new ServicePackage(UUID.randomUUID(), sc3, "Hybrid Battery Health", "Scan & Report", "Efficiency check.", new BigDecimal("4000.00"), 45, true, LocalDateTime.now(), "system", LocalDateTime.now(), "system"));
                
                servicePackageRepository.saveAll(pkgs);
                System.out.println(">>> Expanded Service Packages created.");
            }
        }

        // 4. Financial Records
        if (invoiceRepository.count() < 10) {
            System.out.println(">>> Repositories active: Invoice [" + invoiceRepository.count() + "], Payments [" + paymentRecordRepository.count() + "]");
        }

        System.out.println("--- [INITIALIZER] SEEDING COMPLETE ---");
    }
}
