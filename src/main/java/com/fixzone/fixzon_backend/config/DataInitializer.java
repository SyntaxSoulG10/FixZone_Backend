package com.fixzone.fixzon_backend.config;

import com.fixzone.fixzon_backend.model.*;
import com.fixzone.fixzon_backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component

public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final ServiceCenterRepository serviceCenterRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final NotificationRepository notificationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final AnalyticsRepository analyticsRepository;
    private final BookingHistoryRepository bookingHistoryRepository;
    private final PaymentRepository paymentRepository;
    private final OwnerRepository ownerRepository;
    private final CustomerRepository customerRepository;
    private final ManagerRepository managerRepository;
    private final SuperAdminRepository superAdminRepository;
    private final PasswordEncoder passwordEncoder;
    private final DataSource dataSource;
    private final SubscriptionBillingRepository subscriptionBillingRepository;

    @Value("${spring.jpa.hibernate.ddl-auto:update}")
    private String ddlAuto;

    public DataInitializer(UserRepository userRepository, OwnerRepository ownerRepository,
            CustomerRepository customerRepository, ManagerRepository managerRepository,
            SuperAdminRepository superAdminRepository, ServiceCenterRepository serviceCenterRepository,
            ServicePackageRepository servicePackageRepository, BookingRepository bookingRepository,
            InvoiceRepository invoiceRepository, PaymentRecordRepository paymentRecordRepository,
            NotificationRepository notificationRepository, SubscriptionRepository subscriptionRepository,
            SubscriptionPlanRepository planRepository,
            AnalyticsRepository analyticsRepository, BookingHistoryRepository bookingHistoryRepository,
            PaymentRepository paymentRepository, PasswordEncoder passwordEncoder,
            DataSource dataSource, SubscriptionBillingRepository subscriptionBillingRepository) {
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
        this.planRepository = planRepository;
        this.analyticsRepository = analyticsRepository;
        this.bookingHistoryRepository = bookingHistoryRepository;
        this.paymentRepository = paymentRepository;
        this.passwordEncoder = passwordEncoder;
        this.dataSource = dataSource;
        this.subscriptionBillingRepository = subscriptionBillingRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info(">>> APPLYING SCHEMA MIGRATIONS AND DATA INITIALIZATION <<<");

        try (java.sql.Connection conn = dataSource.getConnection()) {
            java.sql.Statement stmt = conn.createStatement();
            stmt.execute("ALTER TABLE users ALTER COLUMN profile_picture_url TYPE TEXT");
            stmt.execute("ALTER TABLE owner ALTER COLUMN banner_image_url TYPE TEXT");
        } catch (Exception e) {
            log.info("Schema migration note: {}", e.getMessage());
        }

        // DROP auto_renew_enabled column if it exists
        try (java.sql.Connection conn = dataSource.getConnection()) {
            java.sql.Statement stmt = conn.createStatement();
            stmt.execute("ALTER TABLE owner DROP COLUMN IF EXISTS auto_renew_enabled");
            log.info(">>> Dropped auto_renew_enabled column from owner table <<<");
        } catch (Exception e) {
            log.info("Drop auto_renew_enabled column note: {}", e.getMessage());
        }

        // Add model column to vehicles table if it doesn't exist
        try (java.sql.Connection conn = dataSource.getConnection()) {
            java.sql.Statement stmt = conn.createStatement();
            stmt.execute("ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS model VARCHAR(100)");
            stmt.execute(
                    "ALTER TABLE notifications ADD COLUMN IF NOT EXISTS is_archived BOOLEAN NOT NULL DEFAULT FALSE");
        } catch (Exception e) {
            log.info("Column migration note: {}", e.getMessage());
        }

        // DATA REPAIR: Fix any owners in DB who have null subscription_status
        // This ensures all seeded/manually-inserted owners are visible to customers
        try (java.sql.Connection conn = dataSource.getConnection()) {
            java.sql.Statement stmt = conn.createStatement();
            int fixed = stmt.executeUpdate(
                    "UPDATE owner SET subscription_status = 'ACTIVE', trial_ends_at = NOW() + INTERVAL '335 days' " +
                            "WHERE subscription_status IS NULL OR subscription_status = ''");
            if (fixed > 0) {
                log.info(">>> REPAIRED {} owner(s) with null subscription_status → set to ACTIVE <<<", fixed);
            }
        } catch (Exception e) {
            log.info("Subscription status repair note: {}", e.getMessage());
        }

        // CLEAR DUMMY PROFILE PHOTO: Remove the bearded man placeholder from existing
        // test accounts
        try (java.sql.Connection conn = dataSource.getConnection()) {
            java.sql.Statement stmt = conn.createStatement();
            int fixed = stmt.executeUpdate(
                    "UPDATE users SET profile_picture_url = NULL " +
                            "WHERE profile_picture_url LIKE '%unsplash.com/photo-1472099645785%'");
            if (fixed > 0) {
                log.info(">>> REPAIRED {} user(s) with dummy bearded man profile photo → set to NULL <<<", fixed);
            }
        } catch (Exception e) {
            log.info("Clear dummy photo note: {}", e.getMessage());
        }

        // SAFETY GUARD: Only wipe and re-seed if explicitly in 'create' mode.
        // Never delete data just because count() returned 0 — Neon cold start can cause
        // a transient 0 count on the first query before the connection pool warms up.
        boolean isCreateMode = "create".equalsIgnoreCase(ddlAuto);
        long userCount = userRepository.count();

        if (!isCreateMode || userCount > 0) {
            log.info("Existing data found (count={}) or not in create mode. Ensuring seed data only...", userCount);
            ensureMockCharlie();
            ensureRajaMotors();
            ensureMockManager();
            ensureMockPackages();
            ensureSuperAdmins();
            return;
        }

        log.info("--- FRESH INSTALL: CLEARING OLD DATA AND STARTING FRESH SEEDING ---");

        analyticsRepository.deleteAll();
        subscriptionBillingRepository.deleteAll();
        subscriptionRepository.deleteAll();
        planRepository.deleteAll();
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
        // Raja Motors - Specifically requested by the user
        Owner rajaOwner = new Owner(
                UUID.fromString("32fc2f2c-474a-48e2-9cc6-d1473ff122db"),
                "Raja Owner",
                "raja@motors.lk",
                "+94771234567",
                passwordEncoder.encode("pass123"),
                "ROLE_COMPANY_OWNER",
                true,
                LocalDateTime.now(),
                LocalDateTime.now(),
                "system",
                LocalDateTime.now(),
                "system",
                "https://i.pravatar.cc/150?u=raja",
                "FIX001",
                "Raja Motors",
                "contact@rajamotors.lk",
                "+94112000000",
                "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab",
                "https://facebook.com/rajamotors", "https://twitter.com/rajamotors", "https://instagram.com/rajamotors",
                null, false, "ACTIVE", LocalDateTime.now().plusDays(335), null, null, null);
        owners.add(rajaOwner);

        // Tharindu Perera
        owners.add(new Owner(UUID.randomUUID(), "Tharindu Perera", "owner2@fixzone.lk", "+94771000001",
                passwordEncoder.encode("pass123"), "ROLE_COMPANY_OWNER", true, LocalDateTime.now(), LocalDateTime.now(),
                "system", LocalDateTime.now(), "system", "https://i.pravatar.cc/150", "FIX002", "Perera Motors",
                "contact@perera.lk", "+94112000001", "https://images.unsplash.com",
                "https://facebook.com/pereramotors", "https://twitter.com/pereramotors",
                "https://instagram.com/pereramotors",
                null, false, "ACTIVE", LocalDateTime.now().plusDays(335), null, null, null));

        ownerRepository.saveAll(owners);

        // First, seed some Subscription Plans
        SubscriptionPlan basicPlan = new SubscriptionPlan(UUID.randomUUID(), "Basic", new BigDecimal("5000"),
                "Basic Plan", 1, true,
                java.util.List.of("Basic Service Center Profile", "Up to 5 Managers"), false);
        SubscriptionPlan premiumPlan = new SubscriptionPlan(UUID.randomUUID(), "Premium", new BigDecimal("35000"),
                "Premium Plan", 1, true,
                java.util.List.of("Multiple Locations", "API Access"), false);
        planRepository.save(basicPlan);
        planRepository.save(premiumPlan);

        // Seed Subscriptions for the newly created owners
        for (Owner owner : owners) {
            Subscription sub = new Subscription();
            sub.setOwner(owner);
            sub.setStartDate(java.time.LocalDate.now().minusDays(30));
            sub.setEndDate(java.time.LocalDate.now().plusDays(335));
            sub.setPlan(Math.random() > 0.5 ? premiumPlan : basicPlan);
            sub.setStatus("ACTIVE");
            sub.setBillingHistory("Initial subscription activated on " + sub.getStartDate() + " via system seeding.");
            sub = subscriptionRepository.save(sub);

            if (owner.getEmail().equals("raja@motors.lk")) {
                for (int m = 1; m <= 3; m++) {
                    SubscriptionBilling sb = new SubscriptionBilling();
                    sb.setSubscriptionId(sub.getId());
                    sb.setAmount(sub.getPlan().getPrice());
                    sb.setPaymentDate(LocalDateTime.now().minusMonths(m));
                    sb.setStatus("Paid");
                    sb.setMethod("Visa **** 4242");
                    sb.setInvoiceId("INV-2024-00" + m);
                    subscriptionBillingRepository.save(sb);
                }
            }
        }

        for (int i = 0; i < owners.size(); i++) {
            Owner owner = owners.get(i);
            UUID scId = UUID.fromString("11111111-1111-1111-1111-11111111111" + (i + 1));
            ServiceCenter sc = new ServiceCenter(scId, owner, owner.getCompanyName() + " HQ", "Colombo",
                    "+9411400", "08:00 - 18:00", new BigDecimal("4.5"), true, LocalDateTime.now(), "system",
                    LocalDateTime.now(), "system", new String[] { "Toyota", "Nissan" }, null, null, "APPROVED", null,
                    null, null,
                    null, null);
            serviceCenterRepository.save(sc);

            UUID pkgId = UUID.fromString("22222222-2222-2222-2222-22222222222" + (i + 1));
            ServicePackage p = new ServicePackage(pkgId, sc, "Full Service", "Package", null, "Oil & Filter",
                    new BigDecimal("15000.00"), 120, true, LocalDateTime.now(), "system", LocalDateTime.now(),
                    "system");
            servicePackageRepository.save(p);

            // Also ensure the Bike Package from frontend mock exists
            UUID bikePkgId = UUID.fromString("4aba5910-a686-49db-9dde-915c8b7f538c");
            if (!servicePackageRepository.existsById(bikePkgId)) {
                ServicePackage bikePkg = new ServicePackage(bikePkgId, sc, "Gold Package (Bike)", "Package", "BIKE",
                        "Bike specialized care",
                        new BigDecimal("8000.00"), 240, true, LocalDateTime.now(), "system", LocalDateTime.now(),
                        "system");
                servicePackageRepository.save(bikePkg);
            }
        }

        log.info("--- DATA SEEDING COMPLETE ---");

        ensureMockCharlie();
        ensureRajaMotors();
        ensureMockManager();
        ensureMockPackages();
        ensureSuperAdmins();

        // FORCE SEED BILLING HISTORY IF EMPTY (Temporary fix)
        if (subscriptionBillingRepository.count() == 0) {
            log.info(">>> FORCE SEEDING BILLING HISTORY FOR ALL SUBSCRIPTIONS <<<");
            List<Subscription> allSubs = subscriptionRepository.findAll();
            for (Subscription sub : allSubs) {
                SubscriptionBilling sb = new SubscriptionBilling();
                sb.setSubscriptionId(sub.getId());
                sb.setAmount(new BigDecimal("14900.00"));
                sb.setPaymentDate(LocalDateTime.now().minusDays(10));
                sb.setStatus("Paid");
                sb.setMethod("MasterCard **** 1234");
                sb.setInvoiceId("INV-TEST-005");
                subscriptionBillingRepository.save(sb);
            }
            log.info(">>> FORCE SEEDING COMPLETE <<<");
        }
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
                    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?q=80&w=256&h=256&auto=format&fit=crop",
                    "MGR-001",
                    firstCenter.getCenterId());
            managerRepository.save(manager);
            log.info(">>> Mock Manager created successfully <<<");
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
            log.info(">>> Mock Charlie Customer created successfully <<<");
        } else {
            // Ensure password is always in sync with what the code expects
            userRepository.findById(charlieId).ifPresent(u -> {
                u.setPasswordHash(passwordEncoder.encode("FixZone@2026!Secure"));
                userRepository.save(u);
                log.info(">>> Mock Charlie password synced <<<");
            });
        }
    }

    private void ensureMockPackages() {
        System.out.println(">>> Ensuring Mock Packages exist for frontend consistency <<<");
        serviceCenterRepository.findAll().stream().findFirst().ifPresent(sc -> {
            // Full Service
            UUID fullPkgId = UUID.fromString("22222222-2222-2222-2222-222222222221");
            if (!servicePackageRepository.existsById(fullPkgId)) {
                servicePackageRepository
                        .save(new ServicePackage(fullPkgId, sc, "Full Service", "Package", null, "Oil & Filter",
                                new BigDecimal("15000.00"), 120, true, LocalDateTime.now(), "system",
                                LocalDateTime.now(), "system"));
            }

            // Bike Package
            UUID bikePkgId = UUID.fromString("4aba5910-a686-49db-9dde-915c8b7f538c");
            if (!servicePackageRepository.existsById(bikePkgId)) {
                servicePackageRepository.save(new ServicePackage(bikePkgId, sc, "Gold Package (Bike)", "Package",
                        "BIKE", "Bike specialized care",
                        new BigDecimal("8000.00"), 240, true, LocalDateTime.now(), "system", LocalDateTime.now(),
                        "system"));
            }
        });
    }

    private void ensureRajaMotors() {
        UUID rajaId = UUID.fromString("32fc2f2c-474a-48e2-9cc6-d1473ff122db");
        String rajaEmail = "raja@motors.lk";

        // Check if user already exists by ID or Email
        log.info("Checking for Raja Motors (ID: {}, Email: {})...", rajaId, rajaEmail);
        if (!userRepository.existsById(rajaId) && !userRepository.findByEmail(rajaEmail).isPresent()) {
            log.info("Raja Motors not found. Creating new Owner...");
            Owner rajaOwner = new Owner(
                    rajaId,
                    "Raja Owner",
                    rajaEmail,
                    "+94771234567",
                    passwordEncoder.encode("pass123"),
                    "ROLE_COMPANY_OWNER",
                    true,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    "system",
                    LocalDateTime.now(),
                    "system",
                    "https://i.pravatar.cc/150?u=raja",
                    "RAJA001",
                    "Raja Motors",
                    "contact@rajamotors.lk",
                    "+94112000000",
                    "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab",
                    "https://facebook.com/rajamotors", "https://twitter.com/rajamotors",
                    "https://instagram.com/rajamotors",
                    null, false, "ACTIVE", LocalDateTime.now().plusDays(335), null, null, null);
            ownerRepository.save(rajaOwner);
            seedRajaMotorsBranchesAndData(rajaOwner);
            log.info(">>> Raja Motors created and seeded successfully <<<");
        } else {
            log.debug("Raja Motors user/email already exists. Syncing Owner details...");
            Optional<User> existingUser = userRepository.findById(rajaId);
            if (existingUser.isPresent()) {
                Optional<Owner> existingOwner = ownerRepository.findById(rajaId);
                Owner owner;
                if (existingOwner.isPresent()) {
                    owner = existingOwner.get();
                    log.debug("Existing Owner record found.");
                } else {
                    log.debug("User exists but Owner record missing. Creating Owner record...");
                    User user = existingUser.get();
                    owner = new Owner();
                    owner.setUserId(user.getUserId());
                    owner.setFullName(user.getFullName());
                    owner.setEmail(user.getEmail());
                    owner.setPhone(user.getPhone());
                    owner.setPasswordHash(user.getPasswordHash());
                    owner.setRole("ROLE_COMPANY_OWNER");
                    owner.setStatus("Active");
                    owner.setCreatedAt(user.getCreatedAt());
                }

                // Sync essential details for analytics and UI testing
                owner.setOwnerCode("RAJA001");
                owner.setCompanyName("Raja Motors");
                owner.setFacebookUrl("https://facebook.com/rajamotors");
                owner.setTwitterUrl("https://twitter.com/rajamotors");
                owner.setInstagramUrl("https://instagram.com/rajamotors");
                // Always sync password so raja@motors.lk / pass123 always works
                owner.setPasswordHash(passwordEncoder.encode("pass123"));
                // Ensure subscription is active so the owner can log in and use the dashboard
                if (owner.getSubscriptionStatus() == null || "INACTIVE".equals(owner.getSubscriptionStatus())
                        || "EXPIRED".equals(owner.getSubscriptionStatus())) {
                    owner.setSubscriptionStatus("ACTIVE");
                    owner.setTrialEndsAt(LocalDateTime.now().plusDays(335));
                }
                owner.setStatus("Active");
                ownerRepository.save(owner);

                log.debug("Proceeding to seed branches and history for: {}", owner.getEmail());
                seedRajaMotorsBranchesAndData(owner);
            } else {
                log.error("CRITICAL: User ID {} was expected but not found in userRepository!", rajaId);
            }
            log.info(">>> Raja Motors check complete <<<");
        }
    }

    private void seedRajaMotorsBranchesAndData(Owner owner) {
        try {
            int existingCount = serviceCenterRepository.findByOwner_UserId(owner.getUserId()).size();
            long historyCount = bookingRepository.countByTenantId(owner.getUserId());

            log.debug("Seeding Raja Motors - Branches: {}, History: {}", existingCount, historyCount);

            // Always update existing manager images if they are already in the DB
            updateManagerImagesForOwner(owner);
            ensureRajaManagers(owner);

            // SKIP SEEDING if history already exists, but UPDATE metrics to ensure they are
            // accurate
            if (historyCount > 0) {
                log.debug("Raja Motors already has history data. Updating metrics from existing records...");
                updateCustomerMetricsForOwner(owner);
                return;
            }

            log.debug("NO HISTORY FOUND: Seeding history for Raja Motors...");

            log.info("Seeding 3 branches for Raja Motors...");
            // Delete existing ones to start fresh with 3 branches if it was partially
            // seeded
            if (existingCount > 0) {
                List<ServiceCenter> existingCenters = serviceCenterRepository.findByOwner_UserId(owner.getUserId());
                for (ServiceCenter center : existingCenters) {
                    // CLEAR DATA IN REVERSE ORDER OF CONSTRAINTS
                    paymentRecordRepository.deleteAll(paymentRecordRepository.findByCenterId(center.getCenterId()));
                    invoiceRepository.deleteAll(invoiceRepository.findByCenterId(center.getCenterId()));
                    bookingRepository.deleteAll(bookingRepository.findByCenterId(center.getCenterId()));
                }
                if (existingCount < 3) {
                    serviceCenterRepository.deleteAll(existingCenters);
                    log.info("Cleaned up incomplete centers for re-seed.");
                }
            }

            String[] locations = { "Colombo", "Kandy", "Galle" };
            List<ServiceCenter> centers = new ArrayList<>();
            List<ServicePackage> packages = new ArrayList<>();
            List<Manager> managers = new ArrayList<>();

            if (serviceCenterRepository.findByOwner_UserId(owner.getUserId()).size() < 3) {
                for (String loc : locations) {
                    ServiceCenter sc = new ServiceCenter(UUID.randomUUID(), owner, "Raja Motors - " + loc, loc,
                            "+94112000" + loc.length(), "08:00 - 18:00", new BigDecimal("4.5"), true,
                            LocalDateTime.now(), "system",
                            LocalDateTime.now(), "system", new String[] { "Toyota", "Honda", "Nissan", "Suzuki" },
                            null, null, "APPROVED", null, null, null, null, null);
                    centers.add(serviceCenterRepository.save(sc));

                    // Add 3 distinct packages per center for variety
                    packages.add(servicePackageRepository
                            .save(new ServicePackage(UUID.randomUUID(), sc, "Basic Service", "Base maintenance", null,
                                    "Essential oil and filter change.", new BigDecimal("8500.00"), 60, true,
                                    LocalDateTime.now(), "system", LocalDateTime.now(), "system")));

                    packages.add(servicePackageRepository.save(new ServicePackage(UUID.randomUUID(), sc,
                            "Premium Full Service", "Full maintenance package", null,
                            "Oil change, filter, brake check, engine scan.", new BigDecimal("15500.00"), 120, true,
                            LocalDateTime.now(), "system", LocalDateTime.now(), "system")));

                    packages.add(servicePackageRepository.save(new ServicePackage(UUID.randomUUID(), sc,
                            "Interior & Exterior Detail", "Deep cleaning", null,
                            "Full body wash, vacuum, and wax.", new BigDecimal("5500.00"), 90, true,
                            LocalDateTime.now(), "system", LocalDateTime.now(), "system")));

                    String mgrImg = "https://images.unsplash.com/photo-1651684215020-f7a5b6610f23?w=600&auto=format&fit=crop&q=60&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8NHx8cHJvZmlsZSUyMHBob3Rvc3xlbnwwfHwwfHx8MA%3D%3D";

                    Manager mgr = new Manager(UUID.randomUUID(), loc + " Branch Manager",
                            "manager." + loc.toLowerCase() + "@raja.lk",
                            "+94771000" + loc.length(), passwordEncoder.encode("manager123"), "ROLE_SERVICE_MANAGER",
                            true,
                            null, LocalDateTime.now(), "system", LocalDateTime.now(), "system",
                            mgrImg, "MGR-" + loc.substring(0, 3).toUpperCase(), sc.getCenterId());
                    managers.add(managerRepository.save(mgr));
                }
            } else {
                centers = serviceCenterRepository.findByOwner_UserId(owner.getUserId());
                // Ensure each center has at least some packages for seeding
                for (ServiceCenter center : centers) {
                    List<ServicePackage> centerPackages = servicePackageRepository
                            .findByServiceCenter_CenterIdAndIsActiveTrue(center.getCenterId());
                    if (centerPackages.isEmpty()) {
                        packages.add(servicePackageRepository.save(new ServicePackage(UUID.randomUUID(), center,
                                "Standard Service", "Base maintenance", null,
                                "Essential checks and oil service.", new BigDecimal("8500.00"), 60, true,
                                LocalDateTime.now(), "system", LocalDateTime.now(), "system")));
                    } else {
                        packages.addAll(centerPackages);
                    }
                }
            }

            // Create some customers
            List<Customer> customers = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                String email = "customer" + i + "@rajamail.com";
                Optional<User> existingUser = userRepository.findByEmail(email);
                if (existingUser.isPresent()) {
                    customers.add((Customer) existingUser.get());
                } else {
                    Customer c = new Customer();
                    c.setUserId(UUID.randomUUID());
                    c.setFullName("Raja Customer " + i);
                    c.setEmail(email);
                    c.setPhone("+9477123456" + i);
                    c.setPasswordHash(passwordEncoder.encode("password123"));
                    c.setRole("ROLE_CUSTOMER");
                    c.setStatus("Active");
                    c.setCreatedAt(LocalDateTime.now().minusMonths(4));
                    c.setCustomerCode("CUST-" + i);
                    customers.add(customerRepository.save(c));
                }
            }

            // Seed 3 months of historical data
            LocalDateTime now = LocalDateTime.now();
            for (int day = 0; day < 90; day++) {
                LocalDateTime bookingDateTime = now.minusDays(day);

                // Generate 2-4 bookings per day across random centers
                int dailyBookings = 2 + (int) (Math.random() * 3);
                for (int i = 0; i < dailyBookings; i++) {
                    ServiceCenter center = centers.get((int) (Math.random() * centers.size()));
                    Customer customer = customers.get((int) (Math.random() * customers.size()));

                    // Pick a random package from this specific center's packages
                    List<ServicePackage> centerPackages = servicePackageRepository
                            .findByServiceCenter_CenterIdAndIsActiveTrue(center.getCenterId());
                    if (centerPackages.isEmpty())
                        continue;
                    ServicePackage pkg = centerPackages.get((int) (Math.random() * centerPackages.size()));

                    Booking b = new Booking();
                    b.setBookingId(UUID.randomUUID());
                    b.setTenantId(owner.getUserId());
                    b.setCenterId(center.getCenterId());
                    b.setCustomerId(customer.getUserId());
                    b.setVehicleId(UUID.randomUUID());
                    b.setPackageId(pkg.getPackageId());
                    b.setBookingDate(bookingDateTime.toLocalDate());
                    b.setBookingTime(LocalTime.of(9 + (int) (Math.random() * 8), 0));

                    // Determine status based on age
                    if (day > 2) {
                        // Older than 2 days -> mostly COMPLETED
                        b.setStatus(com.fixzone.fixzon_backend.enums.BookingStatus.COMPLETED);
                    } else if (day == 0) {
                        // Today -> mostly CONFIRMED or IN_PROGRESS
                        b.setStatus(Math.random() > 0.5 ? com.fixzone.fixzon_backend.enums.BookingStatus.CONFIRMED
                                : com.fixzone.fixzon_backend.enums.BookingStatus.IN_PROGRESS);
                    } else {
                        b.setStatus(com.fixzone.fixzon_backend.enums.BookingStatus.COMPLETED);
                    }

                    b.setEstimatedCost(pkg.getBasePrice());
                    b.setBookingFee(new BigDecimal("1000.00"));
                    b.setBookingFeePaid(true);
                    b.setCreatedAt(bookingDateTime);

                    bookingRepository.save(b);

                    // Update customer visits
                    customer.setVisits((customer.getVisits() == null ? 0 : customer.getVisits()) + 1);

                    // 1. Create Invoice FIRST to satisfy PaymentRecord FK constraint
                    Invoice inv = new Invoice();
                    inv.setInvoiceId(UUID.randomUUID());
                    inv.setCompanyCode(owner.getOwnerCode());
                    inv.setCenterId(center.getCenterId());
                    inv.setBookingId(b.getBookingId());
                    inv.setIssuedToCustomerId(customer.getUserId());
                    inv.setSubtotal(pkg.getBasePrice());
                    inv.setTax(pkg.getBasePrice().multiply(new BigDecimal("0.08"))); // 8% tax
                    inv.setDiscount(BigDecimal.ZERO);
                    inv.setTotal(inv.getSubtotal().add(inv.getTax()));
                    inv.setStatus(b.getStatus() == com.fixzone.fixzon_backend.enums.BookingStatus.COMPLETED ? "PAID"
                            : "PENDING");
                    inv.setIssuedAt(bookingDateTime.plusHours(2));
                    inv.setCreatedAt(bookingDateTime.plusHours(2));
                    invoiceRepository.save(inv);

                    // Update customer total spent if paid
                    if ("PAID".equals(inv.getStatus())) {
                        customer.setTotalSpent(
                                (customer.getTotalSpent() == null ? BigDecimal.ZERO : customer.getTotalSpent())
                                        .add(inv.getTotal()));
                    }
                    customerRepository.save(customer);

                    // 2. Create Online Payment (Booking Fee) linked to Invoice
                    PaymentRecord onlinePayment = new PaymentRecord();
                    onlinePayment.setPaymentId(UUID.randomUUID());
                    onlinePayment.setInvoiceId(inv.getInvoiceId()); // MUST BE SET
                    onlinePayment.setCenterId(center.getCenterId());
                    onlinePayment.setAmount(new BigDecimal("1000.00"));
                    onlinePayment.setMethod("CARD");
                    onlinePayment.setStatus("SUCCESS");
                    onlinePayment.setCreatedAt(bookingDateTime);
                    paymentRecordRepository.save(onlinePayment);

                    // 3. If COMPLETED, create final Cash Payment
                    if (b.getStatus() == com.fixzone.fixzon_backend.enums.BookingStatus.COMPLETED) {
                        // Final Balance Payment
                        PaymentRecord cashPayment = new PaymentRecord();
                        cashPayment.setPaymentId(UUID.randomUUID());
                        cashPayment.setInvoiceId(inv.getInvoiceId());
                        cashPayment.setCenterId(center.getCenterId());
                        cashPayment.setAmount(inv.getTotal().subtract(new BigDecimal("1000.00")));
                        cashPayment.setMethod("CASH");
                        cashPayment.setStatus("SUCCESS");
                        cashPayment.setProcessedAt(bookingDateTime.plusHours(2).plusMinutes(5));
                        cashPayment.setCreatedAt(bookingDateTime.plusHours(2).plusMinutes(5));
                        paymentRecordRepository.save(cashPayment);
                    }
                }
            }
            log.info("[SUCCESS] Raja Motors seeding completed successfully for {}", owner.getEmail());
        } catch (Exception e) {
            log.error("[ERROR] Failed to seed Raja Motors data: {}", e.getMessage(), e);
        }
    }

    private void updateCustomerMetricsForOwner(Owner owner) {
        try {
            // Find all customers who have bookings with this owner's centers
            List<Customer> customers = customerRepository.findAll();
            for (Customer customer : customers) {
                long visits = bookingRepository.findByCustomerId(customer.getUserId()).stream()
                        .filter(b -> b.getTenantId().equals(owner.getUserId()))
                        .count();

                BigDecimal totalSpent = invoiceRepository.findByIssuedToCustomerId(customer.getUserId()).stream()
                        .filter(inv -> "PAID".equalsIgnoreCase(inv.getStatus()))
                        .map(Invoice::getTotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (visits > 0 || totalSpent.compareTo(BigDecimal.ZERO) > 0) {
                    customer.setVisits((int) visits);
                    customer.setTotalSpent(totalSpent);
                    customerRepository.save(customer);
                }
            }
            log.info("[SUCCESS] Customer metrics updated successfully for {}", owner.getCompanyName());
        } catch (Exception e) {
            log.error("[ERROR] Failed to update customer metrics: {}", e.getMessage(), e);
        }
    }

    private void updateManagerImagesForOwner(Owner owner) {
        try {
            String newMgrImg = "https://images.unsplash.com/photo-1651684215020-f7a5b6610f23?w=600&auto=format&fit=crop&q=60&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8NHx8cHJvZmlsZSUyMHBob3Rvc3xlbnwwfHwwfHx8MA%3D%3D";
            List<Manager> managers = managerRepository.findAll();
            for (Manager manager : managers) {
                // If it's a seeded manager (has a center belonging to this owner)
                if (manager.getManagedCenterId() != null) {
                    manager.setProfilePictureUrl(newMgrImg);
                    managerRepository.save(manager);
                }
            }
            log.info("[SUCCESS] Manager images updated to professional Unsplash URL.");
        } catch (Exception e) {
            log.error("[ERROR] Failed to update manager images: {}", e.getMessage(), e);
        }
    }

    private void ensureRajaManagers(Owner owner) {
        try {
            String[] locations = { "Colombo", "Kandy", "Galle" };
            String mgrImg = "https://images.unsplash.com/photo-1651684215020-f7a5b6610f23?w=600&auto=format&fit=crop&q=60&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8NHx8cHJvZmlsZSUyMHBob3Rvc3xlbnwwfHwwfHx8MA%3D%3D";
            List<ServiceCenter> centers = serviceCenterRepository.findByOwner_UserId(owner.getUserId());
            for (ServiceCenter sc : centers) {
                for (String loc : locations) {
                    if (sc.getName() != null && sc.getName().contains(loc)) {
                        String email = "manager." + loc.toLowerCase() + "@raja.lk";
                        if (!userRepository.existsByEmail(email)) {
                            Manager mgr = new Manager(UUID.randomUUID(), loc + " Branch Manager",
                                    email,
                                    "+94771000" + loc.length(), passwordEncoder.encode("manager123"), "ROLE_SERVICE_MANAGER",
                                    true,
                                    null, LocalDateTime.now(), "system", LocalDateTime.now(), "system",
                                    mgrImg, "MGR-" + loc.substring(0, 3).toUpperCase(), sc.getCenterId());
                            managerRepository.save(mgr);
                            log.info(">>> Seeded Raja Manager for {} ({}) <<<", loc, email);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("[ERROR] Failed to ensure Raja branch managers: {}", e.getMessage(), e);
        }
    }

    private void ensureSuperAdmins() {
        if (!userRepository.existsByEmail("admin1@fixzone.lk")) {
            SuperAdmin admin = new SuperAdmin(
                    UUID.randomUUID(),
                    "Super Admin 1",
                    "admin1@fixzone.lk",
                    "+94770000001",
                    passwordEncoder.encode("FixzoneAdmin!2026"),
                    "ROLE_SUPER_ADMIN",
                    true,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    "system",
                    LocalDateTime.now(),
                    "system",
                    null,
                    "SA-001");
            superAdminRepository.save(admin);
            log.info(">>> Mock Super Admin created successfully <<<");
        }
    }
}
