package com.fixzone.fixzon_backend.config;

import com.fixzone.fixzon_backend.model.*;
import com.fixzone.fixzon_backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ServiceCenterRepository serviceCenterRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final NotificationRepository notificationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AnalyticsRepository analyticsRepository;
    private final BookingHistoryRepository bookingHistoryRepository;
    private final PaymentRepository paymentRepository;
    private final OwnerRepository ownerRepository;
    private final CustomerRepository customerRepository;
    private final ManagerRepository managerRepository;
    private final SuperAdminRepository superAdminRepository;
    private final PasswordEncoder passwordEncoder;
    private final DataSource dataSource;

    @Value("${spring.jpa.hibernate.ddl-auto:update}")
    private String ddlAuto;

    public DataInitializer(UserRepository userRepository, OwnerRepository ownerRepository,
            CustomerRepository customerRepository, ManagerRepository managerRepository,
            SuperAdminRepository superAdminRepository, ServiceCenterRepository serviceCenterRepository,
            ServicePackageRepository servicePackageRepository, BookingRepository bookingRepository,
            InvoiceRepository invoiceRepository, PaymentRecordRepository paymentRecordRepository,
            NotificationRepository notificationRepository, SubscriptionRepository subscriptionRepository,
            AnalyticsRepository analyticsRepository, BookingHistoryRepository bookingHistoryRepository,
            PaymentRepository paymentRepository, PasswordEncoder passwordEncoder, 
            DataSource dataSource) {
        this.userRepository = userRepository;
        this.ownerRepository = ownerRepository;
        this.customerRepository = customerRepository;
        this.managerRepository = managerRepository;
        this.superAdminRepository = superAdminRepository;
        this.serviceCenterRepository = serviceCenterRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.bookingRepository = bookingRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.notificationRepository = notificationRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.analyticsRepository = analyticsRepository;
        this.bookingHistoryRepository = bookingHistoryRepository;
        this.paymentRepository = paymentRepository;
        this.passwordEncoder = passwordEncoder;
        this.dataSource = dataSource;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println(">>> APPLYING SCHEMA MIGRATIONS AND DATA INITIALIZATION <<<");

        try (java.sql.Connection conn = dataSource.getConnection()) {
            java.sql.Statement stmt = conn.createStatement();
            stmt.execute("ALTER TABLE users ALTER COLUMN profile_picture_url TYPE TEXT");
            stmt.execute("ALTER TABLE owner ALTER COLUMN banner_image_url TYPE TEXT");
            stmt.execute("ALTER TABLE payments ALTER COLUMN booking_id TYPE bigint USING (booking_id::text::bigint)");
        } catch (Exception e) {
            System.out.println("Schema migration note: " + e.getMessage());
        }

        if (!"create".equalsIgnoreCase(ddlAuto) && userRepository.count() > 0) {
            System.out.println("Existing data found, ensuring Mock Charlie Customer exists...");
            ensureMockCharlie();
            ensureMockManager();
            return;
        }

        System.out.println("--- CLEARING OLD DATA AND STARTING FRESH SEEDING ---");

        analyticsRepository.deleteAll();
        subscriptionRepository.deleteAll();
        notificationRepository.deleteAll();
        paymentRecordRepository.deleteAll();
        paymentRepository.deleteAll();
        invoiceRepository.deleteAll();
        bookingHistoryRepository.deleteAll();
        bookingRepository.deleteAll();
        servicePackageRepository.deleteAll();
        serviceCenterRepository.deleteAll();
        managerRepository.deleteAll();
        customerRepository.deleteAll();
        ownerRepository.deleteAll();
        superAdminRepository.deleteAll();
        userRepository.deleteAll();

        // ensureMockCharlie(); // Removed

        List<SuperAdmin> superAdmins = new ArrayList<>();
        String[] adminNames = { "Aruna Kumara", "Ruwan Silva", "Gihan Fernando" };
        for (int i = 0; i < adminNames.length; i++) {
            superAdmins.add(new SuperAdmin(UUID.randomUUID(), adminNames[i], "admin" + (i + 1) + "@fixzone.lk",
                    "+9411555000" + i, passwordEncoder.encode("FixZone@2026!Secure"), "ROLE_SUPER_ADMIN", true,
                    LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system",
                    "https://i.pravatar.cc/150?u=" + adminNames[i].replace(" ", "+"), "ADM-00" + (i + 1)));
        }
        superAdminRepository.saveAll(superAdmins);

        List<Owner> owners = new ArrayList<>();
        String[] ownerNames = { "Janaka Ranasinghe", "Tharindu Perera" };
        for (int i = 0; i < ownerNames.length; i++) {
            owners.add(new Owner(UUID.randomUUID(), ownerNames[i], "owner" + (i + 1) + "@fixzone.lk", "+9477100000" + i,
                    passwordEncoder.encode("FixZone@2026!Secure"), "OWNER", true, LocalDateTime.now(), LocalDateTime.now(),
                    "system", LocalDateTime.now(), "system", "https://i.pravatar.cc/150", "FIX00" + (i + 1), "Motors",
                    "contact@motors.lk", "+9411200000" + i, "https://images.unsplash.com"));
        }
        ownerRepository.saveAll(owners);

        for (int i = 0; i < owners.size(); i++) {
            Owner owner = owners.get(i);
            UUID scId = UUID.fromString("11111111-1111-1111-1111-11111111111" + (i + 1));
            ServiceCenter sc = new ServiceCenter(scId, owner, owner.getCompanyName() + " HQ", "Colombo",
                    "+9411400", "08:00 - 18:00", new BigDecimal("4.5"), true, LocalDateTime.now(), "system",
                    LocalDateTime.now(), "system", new String[] { "Toyota", "Nissan" }, "APPROVED", null, null, null, null, null);
            serviceCenterRepository.save(sc);

            UUID pkgId = UUID.fromString("22222222-2222-2222-2222-22222222222" + (i + 1));
            ServicePackage p = new ServicePackage(pkgId, sc, "Full Service", "Package", "Oil & Filter", 
                    new BigDecimal("15000.00"), 120, true, LocalDateTime.now(), "system", LocalDateTime.now(), "system");
            servicePackageRepository.save(p);
        }

        System.out.println("--- DATA SEEDING COMPLETE ---");
        
        ensureMockManager();
    }

    private void ensureMockManager() {
        if (!userRepository.existsByEmail("manager1@fixzone.lk") && serviceCenterRepository.count() > 0) {
            ServiceCenter firstCenter = serviceCenterRepository.findAll().get(0);
            Manager manager = new Manager(
                UUID.randomUUID(),
                "Roshan Wijesinghe",
                "manager1@fixzone.lk",
                "+94772000000",
                passwordEncoder.encode("FixzoneManager!2026"),
                "ROLE_SERVICE_MANAGER",
                true,
                LocalDateTime.now(),
                LocalDateTime.now(),
                "system",
                LocalDateTime.now(),
                "system",
                "https://i.pravatar.cc/150",
                "MGR-001",
                firstCenter.getCenterId()
            );
            managerRepository.save(manager);
            System.out.println(">>> Mock Manager created successfully <<<");
        }
    }

    private void ensureMockCharlie() {
        UUID charlieId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        if (!userRepository.existsById(charlieId)) {
            Customer charlie = new Customer();
            charlie.setUserId(charlieId);
            charlie.setEmail("charlie@example.com");
            charlie.setFullName("Charlie Customer");
            charlie.setRole("ROLE_CUSTOMER");
            charlie.setPasswordHash(passwordEncoder.encode("FixZone@2026!Secure"));
            charlie.setStatus("Active");
            charlie.setCustomerCode("CUST-MOCK");
            customerRepository.save(charlie);
            System.out.println(">>> Mock Charlie Customer created successfully <<<");
        }
    }
}
