package com.fixzone.fixzon_backend.config;

import com.fixzone.fixzon_backend.model.*;
import com.fixzone.fixzon_backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Profile("!test")

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
    private final VehicleRepository vehicleRepository;
    private final BookingStatusHistoryRepository bookingStatusHistoryRepository;

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
            DataSource dataSource, SubscriptionBillingRepository subscriptionBillingRepository,
            VehicleRepository vehicleRepository,
            BookingStatusHistoryRepository bookingStatusHistoryRepository) {
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
        this.vehicleRepository = vehicleRepository;
        this.bookingStatusHistoryRepository = bookingStatusHistoryRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info(">>> APPLYING SCHEMA MIGRATIONS AND DATA INITIALIZATION <<<");

        try (java.sql.Connection conn = dataSource.getConnection()) {
            java.sql.Statement stmt = conn.createStatement();
            stmt.execute("ALTER TABLE users ALTER COLUMN profile_picture_url TYPE TEXT");
            stmt.execute("ALTER TABLE owner ALTER COLUMN banner_image_url TYPE TEXT");
            stmt.execute("ALTER TABLE service_centers ALTER COLUMN business_reg_url TYPE TEXT");
            stmt.execute("ALTER TABLE service_centers ALTER COLUMN nic_url TYPE TEXT");
            stmt.execute("ALTER TABLE service_centers ALTER COLUMN tax_id_url TYPE TEXT");
            stmt.execute("ALTER TABLE vehicles ALTER COLUMN image_url TYPE TEXT");
            stmt.execute("ALTER TABLE service_packages ALTER COLUMN type TYPE TEXT");
            stmt.execute("ALTER TABLE service_centers ADD COLUMN IF NOT EXISTS image_url TEXT");
            stmt.execute("ALTER TABLE service_centers ADD COLUMN IF NOT EXISTS service_lanes_count INTEGER DEFAULT 1");
            stmt.execute("ALTER TABLE bookings ADD COLUMN IF NOT EXISTS duration_mins INTEGER");
            stmt.execute("ALTER TABLE bookings ADD COLUMN IF NOT EXISTS end_time TIME");
            stmt.execute("ALTER TABLE bookings ADD COLUMN IF NOT EXISTS assigned_lane INTEGER");
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

        // DROP redundant features column from service_packages if it exists
        try (java.sql.Connection conn = dataSource.getConnection()) {
            java.sql.Statement stmt = conn.createStatement();
            // In case features column exists and has content while type is null/empty, preserve it into type
            try {
                stmt.execute("UPDATE service_packages SET type = features WHERE (type IS NULL OR type = '') AND features IS NOT NULL");
            } catch (Exception ignored) {
                // features column may not exist yet or have different structure
            }
            stmt.execute("ALTER TABLE service_packages DROP COLUMN IF EXISTS features");
            log.info(">>> Dropped redundant 'features' column from service_packages table <<<");
        } catch (Exception e) {
            log.info("Drop features column note: {}", e.getMessage());
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

        // DATA REPAIR: Remove "Auto Miraj" from package names
        try (java.sql.Connection conn = dataSource.getConnection()) {
            java.sql.Statement stmt = conn.createStatement();
            stmt.executeUpdate("UPDATE service_packages SET name = TRIM(REGEXP_REPLACE(name, '(?i)Auto\\s*Miraj\\s*[-–:]?\\s*', '', 'g')) WHERE name ILIKE '%Auto Miraj%'");
            stmt.executeUpdate("UPDATE service_packages SET description = TRIM(REGEXP_REPLACE(description, '(?i)Auto\\s*Miraj\\s*[-–:]?\\s*', '', 'g')) WHERE description ILIKE '%Auto Miraj%'");
            log.info(">>> Cleaned up 'Auto Miraj' from package names and descriptions in database <<<");
        } catch (Exception e) {
            log.info("Auto Miraj package name cleanup note: {}", e.getMessage());
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

        // SAFETY GUARD: Only seed data if explicitly in 'create' mode.
        boolean isCreateMode = "create".equalsIgnoreCase(ddlAuto);
        if (!isCreateMode) {
            log.info(">>> DDL auto is '{}' (not 'create'). Skipping all seed data creation. <<<", ddlAuto);
            return;
        }

        long userCount = 0;
        try {
            userCount = userRepository.count();
        } catch (Exception e) {
            log.warn("Could not check user count: {}", e.getMessage());
        }

        if (userCount > 0) {
            log.info("Existing data found (count={}). Skipping seeding.", userCount);
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
            ServiceCenter sc = new ServiceCenter();
            sc.setCenterId(scId);
            sc.setOwner(owner);
            sc.setName(owner.getCompanyName() + " HQ");
            sc.setAddress("Colombo");
            sc.setContactPhone("+9411400");
            sc.setOpeningHours("08:00 - 18:00");
            sc.setRating(new BigDecimal("4.5"));
            sc.setIsActive(true);
            sc.setCreatedAt(LocalDateTime.now());
            sc.setCreatedBy("system");
            sc.setUpdatedAt(LocalDateTime.now());
            sc.setUpdatedBy("system");
            sc.setSupportedVehicleBrands(new String[] { "Toyota", "Nissan" });
            sc.setStatus("APPROVED");
            sc.setImageUrl("https://images.unsplash.com/photo-1613214149922-f1809c99b414?w=800&auto=format&fit=crop&q=80");
            serviceCenterRepository.save(sc);

            UUID pkgId = UUID.fromString("22222222-2222-2222-2222-22222222222" + (i + 1));
            ServicePackage p = new ServicePackage();
            p.setPackageId(pkgId);
            p.setServiceCenter(sc);
            p.setName("Full Service");
            p.setType("Essential engine oil change, Oil filter replacement, Fluid level check");
            p.setVehicleType("CAR");
            p.setVehicleBrand("Toyota");
            p.setDescription("Essential oil and filter change.");
            p.setBasePrice(new BigDecimal("15000.00"));
            p.setEstimatedDurationMins(120);
            p.setIsActive(true);
            p.setCreatedAt(LocalDateTime.now());
            p.setCreatedBy("system");
            p.setUpdatedAt(LocalDateTime.now());
            p.setUpdatedBy("system");
            p.setImageUrl("https://images.unsplash.com/photo-1625047509168-a7026f36de04?q=80&w=600&auto=format&fit=crop");
            servicePackageRepository.save(p);

            // Also ensure the Bike Package from frontend mock exists
            UUID bikePkgId = UUID.fromString("4aba5910-a686-49db-9dde-915c8b7f538c");
            if (!servicePackageRepository.existsById(bikePkgId)) {
                ServicePackage bikePkg = new ServicePackage();
                bikePkg.setPackageId(bikePkgId);
                bikePkg.setServiceCenter(sc);
                bikePkg.setName("Gold Package (Bike)");
                bikePkg.setType("Package");
                bikePkg.setVehicleType("BIKE");
                bikePkg.setVehicleBrand("Honda");
                bikePkg.setDescription("Bike specialized care");
                bikePkg.setBasePrice(new BigDecimal("8000.00"));
                bikePkg.setEstimatedDurationMins(240);
                bikePkg.setIsActive(true);
                bikePkg.setCreatedAt(LocalDateTime.now());
                bikePkg.setCreatedBy("system");
                bikePkg.setUpdatedAt(LocalDateTime.now());
                bikePkg.setUpdatedBy("system");
                bikePkg.setImageUrl("https://images.unsplash.com/photo-1625047509168-a7026f36de04?q=80&w=600&auto=format&fit=crop");
                servicePackageRepository.save(bikePkg);
            }
        }

        log.info("--- DATA SEEDING COMPLETE ---");

        ensureMockCharlie();
        ensureRajaMotors();
        ensureMockManager();
        ensureMockPackages();
        ensureSuperAdmins();
        ensureBookingsForManager();

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
        if (serviceCenterRepository.count() == 0) return;
        ServiceCenter firstCenter = serviceCenterRepository.findAll().get(0);

        Optional<User> existingUser = userRepository.findByEmail("manager1@fixzone.lk");
        if (existingUser.isEmpty()) {
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
        } else {
            Optional<Manager> mgrOpt = managerRepository.findById(existingUser.get().getUserId());
            if (mgrOpt.isPresent()) {
                Manager mgr = mgrOpt.get();
                if (mgr.getManagedCenterId() == null) {
                    mgr.setManagedCenterId(firstCenter.getCenterId());
                    managerRepository.save(mgr);
                    log.info(">>> Assigned managedCenterId to existing manager1@fixzone.lk <<<");
                }
            }
        }
    }

    private void ensureBookingsForManager() {
        Optional<User> mgrUserOpt = userRepository.findByEmail("manager1@fixzone.lk");
        if (mgrUserOpt.isEmpty() && serviceCenterRepository.count() > 0) {
            ensureMockManager();
            mgrUserOpt = userRepository.findByEmail("manager1@fixzone.lk");
        }
        if (mgrUserOpt.isEmpty() || serviceCenterRepository.count() == 0) {
            return;
        }

        Manager manager;
        if (mgrUserOpt.get() instanceof Manager) {
            manager = (Manager) mgrUserOpt.get();
        } else {
            Optional<Manager> mOpt = managerRepository.findById(mgrUserOpt.get().getUserId());
            if (mOpt.isPresent()) {
                manager = mOpt.get();
            } else {
                return;
            }
        }

        ServiceCenter center;
        if (manager.getManagedCenterId() != null) {
            center = serviceCenterRepository.findById(manager.getManagedCenterId())
                    .orElse(serviceCenterRepository.findAll().get(0));
        } else {
            center = serviceCenterRepository.findAll().get(0);
            manager.setManagedCenterId(center.getCenterId());
            managerRepository.save(manager);
        }

        // Ensure packages exist for this center
        List<ServicePackage> packages = servicePackageRepository.findByServiceCenter_CenterId(center.getCenterId());
        if (packages.isEmpty()) {
            seedPackagesForCenter(center);
            packages = servicePackageRepository.findByServiceCenter_CenterId(center.getCenterId());
        }
        if (packages.isEmpty()) {
            ServicePackage fallbackPkg1 = new ServicePackage();
            fallbackPkg1.setPackageId(UUID.randomUUID());
            fallbackPkg1.setServiceCenter(center);
            fallbackPkg1.setName("Full Hybrid Periodic Service");
            fallbackPkg1.setType("Maintenance");
            fallbackPkg1.setVehicleBrand("Toyota");
            fallbackPkg1.setDescription("Complete lube, filter, brake scan, and battery inspection.");
            fallbackPkg1.setBasePrice(new BigDecimal("14500.00"));
            fallbackPkg1.setEstimatedDurationMins(90);
            fallbackPkg1.setIsActive(true);
            fallbackPkg1.setCreatedAt(LocalDateTime.now());
            fallbackPkg1.setCreatedBy("system");
            fallbackPkg1.setUpdatedAt(LocalDateTime.now());
            fallbackPkg1.setUpdatedBy("system");
            fallbackPkg1 = servicePackageRepository.save(fallbackPkg1);

            ServicePackage fallbackPkg2 = new ServicePackage();
            fallbackPkg2.setPackageId(UUID.randomUUID());
            fallbackPkg2.setServiceCenter(center);
            fallbackPkg2.setName("Standard Periodic Maintenance & Inspection");
            fallbackPkg2.setType("Inspection");
            fallbackPkg2.setVehicleBrand("Honda");
            fallbackPkg2.setDescription("Engine oil replacement, brake check, multi-point diagnostic check.");
            fallbackPkg2.setBasePrice(new BigDecimal("9500.00"));
            fallbackPkg2.setEstimatedDurationMins(60);
            fallbackPkg2.setIsActive(true);
            fallbackPkg2.setCreatedAt(LocalDateTime.now());
            fallbackPkg2.setCreatedBy("system");
            fallbackPkg2.setUpdatedAt(LocalDateTime.now());
            fallbackPkg2.setUpdatedBy("system");
            fallbackPkg2 = servicePackageRepository.save(fallbackPkg2);

            packages = List.of(fallbackPkg1, fallbackPkg2);
        }

        ServicePackage pkg1 = packages.get(0);
        ServicePackage pkg2 = packages.size() > 1 ? packages.get(1) : pkg1;

        // Helper to ensure customer
        java.util.function.BiFunction<String, String, Customer> getOrCreateCustomer = (email, name) -> {
            Optional<User> uOpt = userRepository.findByEmail(email);
            if (uOpt.isPresent() && uOpt.get() instanceof Customer) {
                return (Customer) uOpt.get();
            } else if (uOpt.isPresent()) {
                return customerRepository.findById(uOpt.get().getUserId()).orElseGet(() -> {
                    Customer c = new Customer();
                    c.setUserId(uOpt.get().getUserId());
                    c.setEmail(email);
                    c.setFullName(name);
                    c.setPhone("+94771234567");
                    c.setRole("ROLE_CUSTOMER");
                    c.setPasswordHash(passwordEncoder.encode("Customer123!"));
                    c.setStatus("Active");
                    c.setCustomerCode("CUST-" + Math.abs(email.hashCode() % 1000));
                    return customerRepository.save(c);
                });
            } else {
                Customer c = new Customer();
                c.setUserId(UUID.randomUUID());
                c.setEmail(email);
                c.setFullName(name);
                c.setPhone("+94771234567");
                c.setRole("ROLE_CUSTOMER");
                c.setPasswordHash(passwordEncoder.encode("Customer123!"));
                c.setStatus("Active");
                c.setCustomerCode("CUST-" + Math.abs(email.hashCode() % 1000));
                return customerRepository.save(c);
            }
        };

        // Helper to ensure vehicle
        java.util.function.Function<Object[], Vehicle> getOrCreateVehicle = (argsArr) -> {
            Customer cust = (Customer) argsArr[0];
            String brand = (String) argsArr[1];
            String plate = (String) argsArr[2];
            String model = (String) argsArr[3];
            List<Vehicle> vList = vehicleRepository.findByCustomerId(cust.getUserId());
            if (!vList.isEmpty()) {
                return vList.get(0);
            }
            Vehicle v = new Vehicle(UUID.randomUUID(), cust.getUserId(), brand, plate, model, "CAR", null, LocalDate.now().minusMonths(3));
            return vehicleRepository.save(v);
        };

        Customer c1 = getOrCreateCustomer.apply("kamal.perera@fixzone.lk", "Kamal Perera");
        Customer c2 = getOrCreateCustomer.apply("nimal.silva@fixzone.lk", "Nimal Silva");

        Vehicle v1 = getOrCreateVehicle.apply(new Object[]{c1, "Toyota", "WP CAB-4521", "Prius"});
        Vehicle v2 = getOrCreateVehicle.apply(new Object[]{c2, "Honda", "WP CAD-7890", "Vezel"});

        UUID tenantId = center.getOwner() != null ? center.getOwner().getUserId() : UUID.randomUUID();
        LocalDate today = LocalDate.now();

        // Only seed sample bookings if no bookings exist for today for this center
        List<Booking> todayBookings = bookingRepository.findByCenterId(center.getCenterId()).stream()
                .filter(b -> today.equals(b.getBookingDate()))
                .toList();

        if (todayBookings.isEmpty()) {

            // Booking 1: IN_PROGRESS (Active morning slot)
            Booking b1 = new Booking();
            b1.setBookingId(UUID.randomUUID());
            b1.setTenantId(tenantId);
            b1.setCenterId(center.getCenterId());
            b1.setCustomerId(c1.getUserId());
            b1.setVehicleId(v1.getId());
            b1.setPackageId(pkg1.getPackageId());
            b1.setBookingDate(today);
            b1.setBookingTime(LocalTime.of(9, 30));
            b1.setStatus(com.fixzone.fixzon_backend.enums.BookingStatus.IN_PROGRESS);
            b1.setEstimatedCost(pkg1.getBasePrice() != null ? pkg1.getBasePrice() : new BigDecimal("14500.00"));
            b1.setBookingFee(new BigDecimal("2000.00"));
            b1.setBookingFeePaid(true);
            b1.setCreatedAt(LocalDateTime.now().minusHours(3));
            b1.setUpdatedAt(LocalDateTime.now().minusMinutes(45));
            b1.setSpecialRequest("Customer: " + c1.getFullName() + ", Vehicle: " + v1.getBrand() + " " + v1.getModel() + ", Vehicle Number: " + v1.getPlateNumber() + ", Service: " + pkg1.getName());
            b1 = bookingRepository.save(b1);
            bookingStatusHistoryRepository.save(new BookingStatusHistory(null, b1.getBookingId(), com.fixzone.fixzon_backend.enums.BookingStatus.PENDING_PAYMENT, b1.getCreatedAt(), "CUSTOMER"));
            bookingStatusHistoryRepository.save(new BookingStatusHistory(null, b1.getBookingId(), com.fixzone.fixzon_backend.enums.BookingStatus.IN_PROGRESS, b1.getUpdatedAt(), "MANAGER"));

            // Booking 2: CONFIRMED (Upcoming afternoon slot 14:00)
            Booking b2 = new Booking();
            b2.setBookingId(UUID.randomUUID());
            b2.setTenantId(tenantId);
            b2.setCenterId(center.getCenterId());
            b2.setCustomerId(c2.getUserId());
            b2.setVehicleId(v2.getId());
            b2.setPackageId(pkg2.getPackageId());
            b2.setBookingDate(today);
            b2.setBookingTime(LocalTime.of(14, 0));
            b2.setStatus(com.fixzone.fixzon_backend.enums.BookingStatus.CONFIRMED);
            b2.setEstimatedCost(pkg2.getBasePrice() != null ? pkg2.getBasePrice() : new BigDecimal("9500.00"));
            b2.setBookingFee(new BigDecimal("1500.00"));
            b2.setBookingFeePaid(true);
            b2.setCreatedAt(LocalDateTime.now().minusHours(2));
            b2.setUpdatedAt(LocalDateTime.now().minusHours(1));
            b2.setSpecialRequest("Customer: " + c2.getFullName() + ", Vehicle: " + v2.getBrand() + " " + v2.getModel() + ", Vehicle Number: " + v2.getPlateNumber() + ", Service: " + pkg2.getName());
            b2 = bookingRepository.save(b2);
            bookingStatusHistoryRepository.save(new BookingStatusHistory(null, b2.getBookingId(), com.fixzone.fixzon_backend.enums.BookingStatus.PENDING_PAYMENT, b2.getCreatedAt(), "CUSTOMER"));
            bookingStatusHistoryRepository.save(new BookingStatusHistory(null, b2.getBookingId(), com.fixzone.fixzon_backend.enums.BookingStatus.CONFIRMED, b2.getUpdatedAt(), "CUSTOMER_PAYMENT"));

            log.info(">>> Seeded exactly 2 today's bookings (1 IN_PROGRESS, 1 CONFIRMED) for manager center {} <<<", center.getName());
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
        }
    }

    private void ensureMockPackages() {
        log.info(">>> Ensuring Standard Automotive Service Packages exist for all Service Centers in Database <<<");
        List<ServiceCenter> centers = serviceCenterRepository.findAll();
        for (ServiceCenter sc : centers) {
            seedPackagesForCenter(sc);
        }
    }

    private void seedPackagesForCenter(ServiceCenter sc) {
        // Automatically assign vehicleBrand to any existing packages in the DB that currently have null/empty brand
        List<ServicePackage> existingList = servicePackageRepository.findByServiceCenter_CenterId(sc.getCenterId());
        for (ServicePackage p : existingList) {
            if (p.getName() != null) {
                String n = p.getName().toLowerCase();
                if (p.getVehicleBrand() == null || p.getVehicleBrand().trim().isEmpty() || p.getVehicleBrand().equalsIgnoreCase("ALL")) {
                    if (n.contains("toyota") || n.contains("platinum") || n.contains("full service") || n.contains("commercial") || n.contains("van") || n.contains("hybrid")) {
                        p.setVehicleBrand("Toyota");
                        if (p.getVehicleType() == null) p.setVehicleType(n.contains("van") ? "VAN" : "CAR");
                    } else if (n.contains("honda") || n.contains("vtec") || n.contains("gold") || n.contains("lube") || n.contains("premium")) {
                        p.setVehicleBrand("Honda");
                        if (p.getVehicleType() == null) p.setVehicleType("CAR");
                    } else if (n.contains("nissan") || n.contains("suv") || n.contains("executive") || n.contains("detail") || n.contains("interior")) {
                        p.setVehicleBrand("Nissan");
                        if (p.getVehicleType() == null) p.setVehicleType("SUV");
                    } else if (n.contains("suzuki") || n.contains("basic") || n.contains("standard") || n.contains("economy") || n.contains("swift")) {
                        p.setVehicleBrand("Suzuki");
                        if (p.getVehicleType() == null) p.setVehicleType("CAR");
                    } else if (n.contains("mitsubishi") || n.contains("pajero") || n.contains("4wd") || n.contains("super select")) {
                        p.setVehicleBrand("Mitsubishi");
                        if (p.getVehicleType() == null) p.setVehicleType("SUV");
                    } else if (n.contains("hyundai") || n.contains("kia") || n.contains("smartstream")) {
                        p.setVehicleBrand("Hyundai");
                        if (p.getVehicleType() == null) p.setVehicleType("CAR");
                    } else if (n.contains("bmw") || n.contains("luxury") || n.contains("european")) {
                        p.setVehicleBrand("BMW");
                        if (p.getVehicleType() == null) p.setVehicleType("CAR");
                    } else if (n.contains("mercedes") || n.contains("benz") || n.contains("star")) {
                        p.setVehicleBrand("Mercedes-Benz");
                        if (p.getVehicleType() == null) p.setVehicleType("CAR");
                    } else if (n.contains("bike") || n.contains("motorcycle") || n.contains("yamaha") || n.contains("scooter")) {
                        p.setVehicleBrand("Yamaha");
                        p.setVehicleType("BIKE");
                    } else {
                        p.setVehicleBrand("Toyota");
                    }
                    servicePackageRepository.save(p);
                }
            }
        }

        // Determine brands supported by this center to decide which packages to seed
        String[] supportedBrands = sc.getSupportedVehicleBrands();
        java.util.Set<String> centerBrands = new java.util.HashSet<>();
        if (supportedBrands != null) {
            for (String b : supportedBrands) {
                if (b != null) {
                    centerBrands.add(b.trim().toLowerCase());
                }
            }
        }
        // If center has no brands configured, default to a couple of common ones to ensure some packages are seeded
        if (centerBrands.isEmpty()) {
            centerBrands.add("toyota");
            centerBrands.add("nissan");
        }

        List<String> keepPackageNames = new java.util.ArrayList<>();

        // 1. Toyota Genuine Periodic Maintenance (Car / Sedan)
        if (centerBrands.contains("toyota")) {
            createPackageIfNotExists(sc, "Toyota Genuine Periodic Maintenance", "CAR", "Toyota",
                    "Engine Oil & Filter Replacement (up to 4L),30-Point Computer ECU Diagnostic Scan,4-Wheel Brake Pad Cleaning & Inspection,Underbody Wash & Anti-Rust Inspection,Coolant & Fluid Top-Up,Interior Cabin Deep Vacuuming,Tire Shine & Alloy Wheel Dressing",
                    "Complete 30-point periodic maintenance covering synthetic engine lubrication, safety diagnostics, 4-wheel brake inspection, and exterior body detailing for Toyota sedans and hatchbacks.",
                    new BigDecimal("18500.00"), 120);
            keepPackageNames.add("Toyota Genuine Periodic Maintenance");
        }

        // 2. Honda VTEC & Hybrid Performance Tune-Up (Car / Sedan)
        if (centerBrands.contains("honda")) {
            createPackageIfNotExists(sc, "Honda VTEC & Hybrid Performance Tune-Up", "CAR", "Honda",
                    "High-Voltage Inverter Coolant Flush,Hybrid Battery Cell Voltage Analysis,30-Point Computer ECU Diagnostic Scan,12V Battery Health & Alternator Test,Electric Brake Actuator Calibration,Spark Plug Calibration & Cleaning",
                    "Specialized hybrid and performance care for Honda Vezel, Fit, Civic, and Accord by certified high-voltage technicians.",
                    new BigDecimal("19500.00"), 120);
            keepPackageNames.add("Honda VTEC & Hybrid Performance Tune-Up");
        }

        // 3. Nissan Executive SUV & 4x4 Major Service (SUV / 4x4)
        if (centerBrands.contains("nissan")) {
            createPackageIfNotExists(sc, "Nissan Executive SUV & 4x4 Major Service", "SUV", "Nissan",
                    "Full Synthetic Oil Replacement (up to 7L),Genuine Oil & Air Filter Replacement,4-Wheel Caliper Greasing & Brake Check,Differential & Transfer Case Fluid Check,Heavy-Duty Underbody Degrease,30-Point Computer ECU Diagnostic Scan",
                    "Heavy-duty periodic service engineered for Nissan Patrol, X-Trail, Qashqai & Navara with genuine filters, drivetrain inspection, and ECU health scanning.",
                    new BigDecimal("26500.00"), 150);
            keepPackageNames.add("Nissan Executive SUV & 4x4 Major Service");
        }

        // 4. Suzuki Express Economy Lube & Filter Care (Car / Hatchback)
        if (centerBrands.contains("suzuki")) {
            createPackageIfNotExists(sc, "Suzuki Express Economy Lube & Filter Care", "CAR", "Suzuki",
                    "Engine Oil Replacement (up to 4L),Genuine Oil Filter Replacement,15-Point Safety Health Check,Windshield Washer Fluid Top-up,Battery Health & Alternator Test,Complimentary Exterior Foam Wash",
                    "Quick-turnaround lube service using OEM Suzuki filters and premium lubricants with safety checks for Alto, Wagon R, Swift & Spacia.",
                    new BigDecimal("9500.00"), 45);
            keepPackageNames.add("Suzuki Express Economy Lube & Filter Care");
        }

        // 5. Mitsubishi Super Select 4WD & Pajero Drivetrain Service (SUV / 4x4)
        if (centerBrands.contains("mitsubishi")) {
            createPackageIfNotExists(sc, "Mitsubishi Super Select 4WD Drivetrain Service", "SUV", "Mitsubishi",
                    "Full Synthetic Oil Replacement (up to 7L),Differential & Transfer Case Fluid Check,Heavy-Duty Underbody Degrease,Suspension Bush & Leaf Spring Test,Brake Caliper Pin Lubrication",
                    "Specialized off-road drivetrain and suspension health service for Mitsubishi Montero, Pajero Sport, and Outlander.",
                    new BigDecimal("27500.00"), 150);
            keepPackageNames.add("Mitsubishi Super Select 4WD Drivetrain Service");
        }

        // 6. Hyundai & Kia Smartstream Engine Diagnostic Care (Car / Sedan)
        if (centerBrands.contains("hyundai") || centerBrands.contains("kia")) {
            createPackageIfNotExists(sc, "Hyundai & Kia Smartstream Engine Care", "CAR", "Hyundai",
                    "30-Point Computer ECU Diagnostic Scan,Engine Oil & Filter Replacement (up to 4L),Spark Plug Check & Calibration,Starter Motor & Charging System Test,Interior Cabin Deep Vacuuming",
                    "Tailored diagnostic calibration and lubrication package for Hyundai Tucson, Elantra, Ioniq, and Kia Sportage & Seltos.",
                    new BigDecimal("17000.00"), 90);
            keepPackageNames.add("Hyundai & Kia Smartstream Engine Care");
        }

        // 7. BMW & European Luxury Precision Diagnostics & Service (Car / Luxury Sedan)
        if (centerBrands.contains("bmw")) {
            createPackageIfNotExists(sc, "BMW & European Luxury Precision Diagnostics", "CAR", "BMW",
                    "Engine Oil & Filter Replacement (up to 4L),30-Point Computer ECU Diagnostic Scan,4-Wheel Brake Pad Cleaning & Inspection,Coolant & Fluid Top-Up,Brake & Clutch Fluid Inspection,Interior Cabin Deep Vacuuming",
                    "Precision diagnostic health check, Condition Based Service (CBS) reset, synthetic LL-04 oil and OEM microfilter replacement for German luxury cars.",
                    new BigDecimal("34500.00"), 150);
            keepPackageNames.add("BMW & European Luxury Precision Diagnostics");
        }

        // 8. Mercedes-Benz Star Diagnostic & Safety Service (Car / Luxury Sedan)
        if (centerBrands.contains("mercedes-benz") || centerBrands.contains("mercedes")) {
            createPackageIfNotExists(sc, "Mercedes-Benz Star Diagnostic & Safety Service", "CAR", "Mercedes-Benz",
                    "30-Point Computer ECU Diagnostic Scan,Engine Oil & Filter Replacement (up to 4L),4-Wheel Brake Pad Cleaning & Inspection,Coolant Top-up & Radiator Test,High-Pressure Underbody Wash & Degrease",
                    "Specialized Star Diagnosis scan, transmission inspection, brake fluid flush, and luxury detailing for Mercedes-Benz C, E, and S-Class.",
                    new BigDecimal("38000.00"), 180);
            keepPackageNames.add("Mercedes-Benz Star Diagnostic & Safety Service");
        }

        // 9. Commercial Van & Passenger Fleet Service (Van / Minibus)
        if (centerBrands.contains("toyota")) {
            createPackageIfNotExists(sc, "Commercial Van & Passenger Fleet Service", "VAN", "Toyota",
                    "Diesel/Petrol Engine Oil (up to 6L),Genuine Oil & Fuel Filter Replacement,Heavy Duty Brake Inspection,Suspension Bush & Leaf Spring Test,Radiator Coolant Flush & Pressure Test,Electrical System Scan",
                    "Tailored for commercial vans and fleet transports (Toyota KDH / HiAce, Nissan Caravan, Every) to maximize operational uptime and fuel efficiency.",
                    new BigDecimal("22000.00"), 120);
            keepPackageNames.add("Commercial Van & Passenger Fleet Service");
        }

        // 10. Pro Motorcycle & Scooter Precision Care (Motorcycle / Scooter)
        if (centerBrands.contains("yamaha") || centerBrands.contains("honda")) {
            createPackageIfNotExists(sc, "Pro Motorcycle & Scooter Precision Care", "BIKE", "Yamaha",
                    "Engine Oil Replacement,Brake Pad & Shoe Inspection,Drive Chain Cleaning & Lubrication,Spark Plug Calibration & Cleaning,Tire Pressure & Safety Check",
                    "Specialized 2-wheeler precision care for scooters and sport bikes (Yamaha, Honda, TVS, Bajaj) with drive chain alignment and brake tuning.",
                    new BigDecimal("6500.00"), 60);
            keepPackageNames.add("Pro Motorcycle & Scooter Precision Care");
        }

        // 11. Universal All-Makes Multi-Point Care (Universal / All Makes)
        createPackageIfNotExists(sc, "Universal All-Makes Multi-Point Care", "CAR", "ALL",
                "Engine Oil Replacement (up to 4L),Genuine Oil Filter Replacement,15-Point Safety Health Check,Windshield Washer Fluid Top-up,Battery Health & Alternator Test,Foam Body Wash & Wax Polish",
                "Universal multi-point safety inspection, engine lubrication, battery testing, and foam wash suitable for all vehicle makes and models.",
                new BigDecimal("11500.00"), 60);
        keepPackageNames.add("Universal All-Makes Multi-Point Care");

    }

    private void createPackageIfNotExists(ServiceCenter sc, String name, String vehicleType, String vehicleBrand, String type, String desc, BigDecimal price, int duration) {
        java.util.Optional<ServicePackage> existingOpt = servicePackageRepository.findByServiceCenter_CenterId(sc.getCenterId())
                .stream().filter(p -> p.getName().equalsIgnoreCase(name)).findFirst();
        if (existingOpt.isPresent()) {
            ServicePackage p = existingOpt.get();
            p.setVehicleType(vehicleType);
            p.setVehicleBrand(vehicleBrand);
            p.setType(type);
            p.setDescription(desc);
            p.setBasePrice(price);
            p.setEstimatedDurationMins(duration);
            p.setIsActive(true);
            servicePackageRepository.save(p);
        } else {
            ServicePackage p = new ServicePackage();
            p.setPackageId(UUID.randomUUID());
            p.setServiceCenter(sc);
            p.setName(name);
            p.setType(type);
            p.setVehicleType(vehicleType);
            p.setVehicleBrand(vehicleBrand);
            p.setDescription(desc);
            p.setBasePrice(price);
            p.setEstimatedDurationMins(duration);
            p.setIsActive(true);
            p.setCreatedAt(LocalDateTime.now());
            p.setCreatedBy("system");
            p.setUpdatedAt(LocalDateTime.now());
            p.setUpdatedBy("system");
            servicePackageRepository.save(p);
        }
    }

    private void ensureRajaMotors() {
        UUID rajaId = UUID.fromString("32fc2f2c-474a-48e2-9cc6-d1473ff122db");
        String rajaEmail = "raja@motors.lk";

        // Check if user already exists by ID or Email
        if (userRepository.existsById(rajaId) || userRepository.existsByEmail(rajaEmail)) {
            log.info("Raja Motors owner already exists. Skipping duplicate seeding.");
            return;
        }

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

        // Ensure 1 default Service Center for Raja Motors
        if (serviceCenterRepository.findByOwner_UserId(rajaOwner.getUserId()).isEmpty()) {
            ServiceCenter sc = new ServiceCenter();
            sc.setCenterId(UUID.randomUUID());
            sc.setOwner(rajaOwner);
            sc.setName("Raja Motors HQ");
            sc.setAddress("Colombo");
            sc.setContactPhone("+94112000000");
            sc.setOpeningHours("08:00 - 18:00");
            sc.setRating(new BigDecimal("4.5"));
            sc.setIsActive(true);
            sc.setCreatedAt(LocalDateTime.now());
            sc.setCreatedBy("system");
            sc.setUpdatedAt(LocalDateTime.now());
            sc.setUpdatedBy("system");
            sc.setSupportedVehicleBrands(new String[] { "Toyota", "Honda", "Nissan", "Suzuki" });
            sc.setStatus("APPROVED");
            sc.setImageUrl("https://images.unsplash.com/photo-1613214149922-f1809c99b414?w=800&auto=format&fit=crop&q=80");
            serviceCenterRepository.save(sc);
            seedPackagesForCenter(sc);
        }
        log.info(">>> Raja Motors created successfully <<<");
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
