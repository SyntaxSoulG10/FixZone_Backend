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
            System.out.println(">>> EXISTING DATA DETECTED, SKIPPING RE-SEEDING <<<");
            ensureMockCharlie();
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

        ensureMockCharlie();

        List<SuperAdmin> superAdmins = new ArrayList<>();
        String[] adminNames = { "Aruna Kumara", "Ruwan Silva", "Gihan Fernando" };
        for (int i = 0; i < adminNames.length; i++) {
            superAdmins.add(new SuperAdmin(UUID.randomUUID(), adminNames[i], "admin" + (i + 1) + "@fixzone.lk",
                    "+9411555000" + i, passwordEncoder.encode("Admin123!"), "ROLE_SUPER_ADMIN", true,
                    LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system",
                    "https://i.pravatar.cc/150?u=" + adminNames[i].replace(" ", "+"), "ADM-00" + (i + 1)));
        }
        superAdminRepository.saveAll(superAdmins);

        List<Owner> owners = new ArrayList<>();
        String[] ownerNames = { "Janaka Ranasinghe", "Tharindu Perera" };
        for (int i = 0; i < ownerNames.length; i++) {
            owners.add(new Owner(UUID.randomUUID(), ownerNames[i], "owner" + (i + 1) + "@fixzone.lk", "+9477100000" + i,
                    passwordEncoder.encode("pass123"), "OWNER", true, LocalDateTime.now(), LocalDateTime.now(),
                    "system", LocalDateTime.now(), "system", "https://i.pravatar.cc/150", "FIX00" + (i + 1), "Motors",
                    "contact@motors.lk", "+9411200000" + i, "https://images.unsplash.com"));
        }
        ownerRepository.saveAll(owners);

        Owner owner1 = owners.get(0);
        Owner owner2 = owners.get(1);

        // Center 1
        ServiceCenter sc1 = new ServiceCenter(UUID.randomUUID(), owner1, "Auto Expert Service Garage", "Colombo 07",
                "+94112345678", "08:00 - 18:00", new BigDecimal("4.8"), true, LocalDateTime.now(), "system",
                LocalDateTime.now(), "system", new String[] { "car", "van" }, "APPROVED", null, null, null, null,
                "https://images.unsplash.com/photo-1625047509168-a71c67c00684?q=80&w=1470&auto=format&fit=crop", null);
        serviceCenterRepository.save(sc1);
        // car packages
        servicePackageRepository.save(new ServicePackage(UUID.randomUUID(), sc1, "Full Service - Car", "Package", "car",
                "Engine oil change, filter replacement, brake check", new BigDecimal("15000.00"), 120, true,
                LocalDateTime.now(), "system", LocalDateTime.now(), "system"));
        servicePackageRepository.save(new ServicePackage(UUID.randomUUID(), sc1, "Express Wash - Car", "Package", "car",
                "Exterior wash, vacuum, wipe-down", new BigDecimal("3500.00"), 45, true, LocalDateTime.now(), "system",
                LocalDateTime.now(), "system"));
        // van packages
        servicePackageRepository.save(new ServicePackage(UUID.randomUUID(), sc1, "Full Service - Van", "Package", "van",
                "Oil change, coolant top-up, tyre rotation", new BigDecimal("18000.00"), 150, true, LocalDateTime.now(),
                "system", LocalDateTime.now(), "system"));

        // Center 2
        ServiceCenter sc2 = new ServiceCenter(UUID.randomUUID(), owner1, "QuickBike Express", "Nugegoda",
                "+94112998877", "07:00 - 20:00", new BigDecimal("4.6"), true, LocalDateTime.now(), "system",
                LocalDateTime.now(), "system", new String[] { "bike" }, "APPROVED", null, null, null, null,
                "https://images.unsplash.com/photo-1558981403-c5f9899a28bc?q=80&w=1470&auto=format&fit=crop", null);
        serviceCenterRepository.save(sc2);
        // bike packages
        servicePackageRepository.save(new ServicePackage(UUID.randomUUID(), sc2, "Bike Standard Wash", "Package",
                "bike", "Full body wash, chain lube, tyre pressure check", new BigDecimal("2500.00"), 30, true,
                LocalDateTime.now(), "system", LocalDateTime.now(), "system"));
        servicePackageRepository.save(new ServicePackage(UUID.randomUUID(), sc2, "Bike Full Service", "Package", "bike",
                "Oil change, air filter, spark plug, brake pads", new BigDecimal("7500.00"), 90, true,
                LocalDateTime.now(), "system", LocalDateTime.now(), "system"));

        // Center 3
        ServiceCenter sc3 = new ServiceCenter(UUID.randomUUID(), owner2, "Heavy Duty Motors", "Peliyagoda",
                "+94113334455", "06:00 - 22:00", new BigDecimal("4.3"), true, LocalDateTime.now(), "system",
                LocalDateTime.now(), "system", new String[] { "van", "lorry" }, "APPROVED", null, null, null, null,
                "https://images.unsplash.com/photo-1517524008697-84bbe3c3fd98?q=80&w=1470&auto=format&fit=crop", null);
        serviceCenterRepository.save(sc3);
        // van packages
        servicePackageRepository.save(new ServicePackage(UUID.randomUUID(), sc3, "Van Inspection", "Package", "van",
                "Full diagnostic scan, brake system check, fluid levels", new BigDecimal("20000.00"), 180, true,
                LocalDateTime.now(), "system", LocalDateTime.now(), "system"));
        // lorry packages
        servicePackageRepository.save(new ServicePackage(UUID.randomUUID(), sc3, "Heavy Lorry Service", "Package",
                "lorry", "Engine tune-up, suspension check, tyre rotation", new BigDecimal("45000.00"), 300, true,
                LocalDateTime.now(), "system", LocalDateTime.now(), "system"));
        servicePackageRepository.save(new ServicePackage(UUID.randomUUID(), sc3, "Lorry Oil & Filter", "Package",
                "lorry", "Heavy duty oil change and dual filter replacement", new BigDecimal("28000.00"), 120, true,
                LocalDateTime.now(), "system", LocalDateTime.now(), "system"));

        // Center 4
        ServiceCenter sc4 = new ServiceCenter(UUID.randomUUID(), owner2, "Elite Auto Care", "Battaramulla",
                "+94114445566", "09:00 - 17:00", new BigDecimal("4.9"), true, LocalDateTime.now(), "system",
                LocalDateTime.now(), "system", new String[] { "car" }, "APPROVED", null, null, null, null,
                "https://images.unsplash.com/photo-1619642751034-765dfdf7c58e?q=80&w=1374&auto=format&fit=crop", null);
        serviceCenterRepository.save(sc4);
        // car packages
        servicePackageRepository.save(new ServicePackage(UUID.randomUUID(), sc4, "Premium Detailing", "Package", "car",
                "Full exterior wax, interior detailing, ceramic coat", new BigDecimal("35000.00"), 240, true,
                LocalDateTime.now(), "system", LocalDateTime.now(), "system"));
        servicePackageRepository.save(new ServicePackage(UUID.randomUUID(), sc4, "Engine Tune-Up", "Package", "car",
                "Spark plugs, air filter, fuel injector clean", new BigDecimal("12000.00"), 90, true,
                LocalDateTime.now(), "system", LocalDateTime.now(), "system"));

        System.out.println("--- DATA SEEDING COMPLETE ---");
    }

    private void ensureMockCharlie() {
        UUID charlieId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        if (!userRepository.existsById(charlieId)) {
            Customer charlie = new Customer();
            charlie.setUserId(charlieId);
            charlie.setEmail("charlie@example.com");
            charlie.setFullName("Charlie Customer");
            charlie.setRole("ROLE_CUSTOMER");
            charlie.setPasswordHash(passwordEncoder.encode("charlie123"));
            charlie.setStatus("Active");
            charlie.setCustomerCode("CUST-MOCK");
            customerRepository.save(charlie);
            System.out.println(">>> Mock Charlie Customer created successfully <<<");
        }
    }
}
