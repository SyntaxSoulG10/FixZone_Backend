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
                        superAdmins.add(new SuperAdmin(UUID.randomUUID(), adminNames[i],
                                        "admin" + (i + 1) + "@fixzone.lk",
                                        "+9411555000" + i, passwordEncoder.encode("Admin123!"), "ROLE_SUPER_ADMIN",
                                        true,
                                        LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(),
                                        "system",
                                        "https://i.pravatar.cc/150?u=" + adminNames[i].replace(" ", "+"),
                                        "ADM-00" + (i + 1)));
                }
                superAdminRepository.saveAll(superAdmins);

                List<Owner> owners = new ArrayList<>();
                String[] ownerNames = { "Janaka Ranasinghe", "Tharindu Perera" };
                for (int i = 0; i < ownerNames.length; i++) {
                        owners.add(new Owner(UUID.randomUUID(), ownerNames[i], "owner" + (i + 1) + "@fixzone.lk",
                                        "+9477100000" + i,
                                        passwordEncoder.encode("pass123"), "OWNER", true, LocalDateTime.now(),
                                        LocalDateTime.now(),
                                        "system", LocalDateTime.now(), "system", "https://i.pravatar.cc/150",
                                        "FIX00" + (i + 1), "Motors",
                                        "contact@motors.lk", "+9411200000" + i, "https://images.unsplash.com"));
                }
                ownerRepository.saveAll(owners);

                Owner owner1 = owners.get(0);
                Owner owner2 = owners.get(1);

                // Center 1
                ServiceCenter sc1 = new ServiceCenter(UUID.randomUUID(), owner1, "Auto Expert Service Garage",
                                "Colombo 07",
                                "+94112345678", "08:00 - 18:00", new BigDecimal("4.8"), true, LocalDateTime.now(),
                                "system",
                                LocalDateTime.now(), "system", new String[] { "car", "van" }, "APPROVED", null, null,
                                null, "2026-04-10",
                                "https://images.unsplash.com/photo-1517524008697-84bbe3c3fd98?q=80&w=1470&auto=format&fit=crop",
                                null, new String[] { "2026-05-10", "2026-05-15" });
                serviceCenterRepository.save(sc1);

                // car packages
                servicePackageRepository.save(new ServicePackage(UUID.randomUUID(), sc1,
                                "Full Service Package (Petrol Cars Only)", "Package", "car",
                                "Oil and filter replacement, spark plug inspection, brake servicing, suspension check, 40-point inspection",
                                new BigDecimal("15000.00"), 120, true,
                                LocalDateTime.now(), "system", LocalDateTime.now(), "system",
                                "https://images.unsplash.com/photo-1583121274602-3e2820c69888?q=80&w=1470&auto=format&fit=crop"));

                servicePackageRepository.save(new ServicePackage(UUID.randomUUID(), sc1,
                                "Exterior Shine Package (Cars Only)", "Package", "car",
                                "Foam wash, premium wax, tire cleaning, glass treatment, scratch minimization",
                                new BigDecimal("3500.00"), 45, true,
                                LocalDateTime.now(), "system", LocalDateTime.now(), "system",
                                "https://images.unsplash.com/photo-1503376780353-7e6692767b70?q=80&w=1470&auto=format&fit=crop"));

                // van packages
                servicePackageRepository.save(new ServicePackage(UUID.randomUUID(), sc1,
                                "Van Service Package (Vans Only)", "Package", "van",
                                "Engine oil and filter change, brake and clutch inspection, cooling system service, electrical check",
                                new BigDecimal("18000.00"), 150, true,
                                LocalDateTime.now(), "system", LocalDateTime.now(), "system",
                                "https://images.unsplash.com/photo-1536700503339-1e4b06520771?q=80&w=1470&auto=format&fit=crop"));

                // Center 2
                ServiceCenter sc2 = new ServiceCenter(UUID.randomUUID(), owner1, "QuickBike Express", "Nugegoda",
                                "+94112998877", "07:00 - 20:00", new BigDecimal("4.6"), true, LocalDateTime.now(),
                                "system",
                                LocalDateTime.now(), "system", new String[] { "bike" }, "APPROVED", null, null, null,
                                null,
                                "https://images.unsplash.com/photo-1558981403-c5f9899a28bc?q=80&w=1470&auto=format&fit=crop",
                                null, new String[] { "2026-05-12" });
                serviceCenterRepository.save(sc2);

                // bike packages
                servicePackageRepository.save(new ServicePackage(UUID.randomUUID(), sc2,
                                "Motorcycle Service Package (Bikes Only)", "Package", "bike",
                                "Engine oil change, chain cleaning and lubrication, brake inspection, tire pressure check",
                                new BigDecimal("2500.00"), 30, true,
                                LocalDateTime.now(), "system", LocalDateTime.now(), "system",
                                "https://images.unsplash.com/photo-1558981403-c5f9899a28bc?q=80&w=1470&auto=format&fit=crop"));

                servicePackageRepository.save(new ServicePackage(UUID.randomUUID(), sc2,
                                "Bike Performance Tune-Up (Bikes Only)", "Package", "bike",
                                "Oil change, air filter service, spark plug replacement, brake pad inspection",
                                new BigDecimal("7500.00"), 90, true,
                                LocalDateTime.now(), "system", LocalDateTime.now(), "system",
                                "https://images.unsplash.com/photo-1515777315835-281b94c9589f?q=80&w=1470&auto=format&fit=crop"));

                // Center 3
                ServiceCenter sc3 = new ServiceCenter(UUID.randomUUID(), owner2, "Heavy Duty Motors", "Peliyagoda",
                                "+94113334455", "06:00 - 22:00", new BigDecimal("4.3"), true, LocalDateTime.now(),
                                "system",
                                LocalDateTime.now(), "system", new String[] { "van", "lorry" }, "APPROVED", null, null,
                                null, null,
                                "https://images.unsplash.com/photo-1517524008697-84bbe3c3fd98?q=80&w=1470&auto=format&fit=crop",
                                null, null);
                serviceCenterRepository.save(sc3);

                // van packages
                servicePackageRepository.save(new ServicePackage(UUID.randomUUID(), sc3,
                                "Commercial Van Inspection (Vans Only)", "Package", "van",
                                "Full diagnostic scan, brake system inspection, cooling system check, fluid level analysis",
                                new BigDecimal("20000.00"), 180, true,
                                LocalDateTime.now(), "system", LocalDateTime.now(), "system",
                                "https://images.unsplash.com/photo-1536700503339-1e4b06520771?q=80&w=1470&auto=format&fit=crop"));

                // lorry packages
                servicePackageRepository.save(new ServicePackage(UUID.randomUUID(), sc3,
                                "Light Truck Service (Lorries Only)", "Package", "lorry",
                                "Engine tune-up, suspension inspection, brake system service, tyre rotation",
                                new BigDecimal("45000.00"), 300, true,
                                LocalDateTime.now(), "system", LocalDateTime.now(), "system",
                                "https://images.unsplash.com/photo-1601584115197-04ecc0da31d7?q=80&w=1470&auto=format&fit=crop"));

                servicePackageRepository.save(new ServicePackage(UUID.randomUUID(), sc3,
                                "Heavy Duty Oil & Filter Service (Lorries Only)", "Package", "lorry",
                                "Heavy-duty oil replacement and dual filter servicing",
                                new BigDecimal("28000.00"), 120, true,
                                LocalDateTime.now(), "system", LocalDateTime.now(), "system",
                                "https://images.unsplash.com/photo-1501700493788-fa1a4fc9fe62?q=80&w=1453&auto=format&fit=crop"));

                // Center 4
                ServiceCenter sc4 = new ServiceCenter(UUID.randomUUID(), owner2, "Elite Auto Care", "Battaramulla",
                                "+94114445566", "09:00 - 17:00", new BigDecimal("4.9"), true, LocalDateTime.now(),
                                "system",
                                LocalDateTime.now(), "system", new String[] { "car" }, "APPROVED", null, null, null,
                                null,
                                "https://images.unsplash.com/photo-1619642751034-765dfdf7c58e?q=80&w=1374&auto=format&fit=crop",
                                null, null);
                serviceCenterRepository.save(sc4);

                // car packages
                servicePackageRepository.save(new ServicePackage(UUID.randomUUID(), sc4,
                                "Premium Total Care (Cars Only)", "Package", "car",
                                "Mechanical inspection, detailing, engine tuning, fluid replacements, 60-point check",
                                new BigDecimal("35000.00"), 240, true,
                                LocalDateTime.now(), "system", LocalDateTime.now(), "system",
                                "https://images.unsplash.com/photo-1619642751034-765dfdf7c58e?q=80&w=1374&auto=format&fit=crop"));

                servicePackageRepository.save(new ServicePackage(UUID.randomUUID(), sc4,
                                "European Car Precision Service (European Cars Only)", "Package", "car",
                                "Synthetic oil service, OEM filters, ECU diagnostics, brake and suspension tuning",
                                new BigDecimal("12000.00"), 90, true,
                                LocalDateTime.now(), "system", LocalDateTime.now(), "system",
                                "https://images.unsplash.com/photo-1533473359331-0135ef1b58bf?q=80&w=1470&auto=format&fit=crop"));

                // Center 5
                ServiceCenter sc5 = new ServiceCenter(UUID.randomUUID(), owner1, "TukTuk & Wheel Pros", "Dehiwala",
                                "+94115556677", "08:30 - 17:30", new BigDecimal("4.5"), true, LocalDateTime.now(),
                                "system",
                                LocalDateTime.now(), "system", new String[] { "three wheels" }, "APPROVED", null, null,
                                null, null,
                                "https://images.unsplash.com/photo-1590856029826-c7a73142bbf1?q=80&w=1473&auto=format&fit=crop",
                                null, null);
                serviceCenterRepository.save(sc5);

                servicePackageRepository.save(new ServicePackage(UUID.randomUUID(), sc5,
                                "Three Wheeler Full Service", "Package", "three wheels",
                                "Engine oil, gear oil, brake adjustment, greasing, preventive maintenance",
                                new BigDecimal("4500.00"), 60, true,
                                LocalDateTime.now(), "system", LocalDateTime.now(), "system",
                                "https://images.unsplash.com/photo-1677981316525-53d7d56d7a04?w=600&auto=format&fit=crop&q=60&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8M3x8dGhyZWUlMjB3aGVlbHxlbnwwfHwwfHx8MA%3D%3D?q=80&w=1470?q=80&w=1374&auto=format&fit=crop"));

                servicePackageRepository.save(new ServicePackage(UUID.randomUUID(), sc5,
                                "Three Wheeler Exterior Refresh", "Package", "three wheels",
                                "Full body wash, interior vacuum, dashboard polish",
                                new BigDecimal("1200.00"), 30, true,
                                LocalDateTime.now(), "system", LocalDateTime.now(), "system",
                                "https://images.unsplash.com/photo-1677981316525-53d7d56d7a04?w=600&auto=format&fit=crop&q=60&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8M3x8dGhyZWUlMjB3aGVlbHxlbnwwfHwwfHx8MA%3D%3D?q=80&w=1470?q=80&w=1374&auto=format&fit=crop"));

                // Center 6
                ServiceCenter sc6 = new ServiceCenter(UUID.randomUUID(), owner2, "City Hub Multi-Service", "Maharagama",
                                "+94118889900", "07:30 - 21:00", new BigDecimal("4.7"), true, LocalDateTime.now(),
                                "system",
                                LocalDateTime.now(), "system", new String[] { "car", "bike", "three wheels" },
                                "APPROVED", null, null, null, null,
                                "https://images.unsplash.com/photo-1486006396193-47106858c9cd?q=80&w=1513&auto=format&fit=crop",
                                null, null);
                serviceCenterRepository.save(sc6);

                servicePackageRepository.save(new ServicePackage(UUID.randomUUID(), sc6,
                                "Eco Friendly Car Wash (Cars Only)", "Package", "car",
                                "Water-saving wash with biodegradable wax and exterior polish",
                                new BigDecimal("4000.00"), 40, true,
                                LocalDateTime.now(), "system", LocalDateTime.now(), "system",
                                "https://images.unsplash.com/photo-1533473359331-0135ef1b58bf?q=80&w=1470&auto=format&fit=crop"));

                servicePackageRepository.save(new ServicePackage(UUID.randomUUID(), sc6,
                                "Bike Chain & Lube Service (Bikes Only)", "Package", "bike",
                                "Chain cleaning, tension adjustment and lubrication",
                                new BigDecimal("1500.00"), 20, true,
                                LocalDateTime.now(), "system", LocalDateTime.now(), "system",
                                "https://images.unsplash.com/photo-1558981403-c5f9899a28bc?q=80&w=1470&auto=format&fit=crop"));

                servicePackageRepository.save(new ServicePackage(UUID.randomUUID(), sc6,
                                "Wheel Alignment Service (Three Wheelers Only)", "Package", "three wheels",
                                "Precision wheel alignment and suspension balancing",
                                new BigDecimal("2500.00"), 45, true,
                                LocalDateTime.now(), "system", LocalDateTime.now(), "system",
                                "https://images.unsplash.com/photo-1677981316525-53d7d56d7a04?w=600&auto=format&fit=crop&q=60&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8M3x8dGhyZWUlMjB3aGVlbHxlbnwwfHwwfHx8MA%3D%3D?q=80&w=1470?q=80&w=1374&auto=format&fit=crop"));
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
