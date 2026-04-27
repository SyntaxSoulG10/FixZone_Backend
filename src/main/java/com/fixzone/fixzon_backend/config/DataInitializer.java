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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
        } catch (Exception e) {
            System.out.println("Schema migration note: " + e.getMessage());
        }

        if (!"create".equalsIgnoreCase(ddlAuto) && userRepository.count() > 0) {
            System.out.println("Existing data found, ensuring Mock Charlie and Raja Motors exist...");
            ensureMockCharlie();
            ensureRajaMotors();
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
                "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab"
        );
        owners.add(rajaOwner);

        // Tharindu Perera
        owners.add(new Owner(UUID.randomUUID(), "Tharindu Perera", "owner2@fixzone.lk", "+94771000001",
                passwordEncoder.encode("pass123"), "ROLE_COMPANY_OWNER", true, LocalDateTime.now(), LocalDateTime.now(),
                "system", LocalDateTime.now(), "system", "https://i.pravatar.cc/150", "FIX002", "Perera Motors",
                "contact@perera.lk", "+94112000001", "https://images.unsplash.com"));
        
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

    private void ensureRajaMotors() {
        UUID rajaId = UUID.fromString("32fc2f2c-474a-48e2-9cc6-d1473ff122db");
        String rajaEmail = "raja@motors.lk";
        
        // Check if user already exists by ID or Email
        System.out.println("Checking for Raja Motors (ID: " + rajaId + ", Email: " + rajaEmail + ")...");
        if (!userRepository.existsById(rajaId) && !userRepository.findByEmail(rajaEmail).isPresent()) {
            System.out.println("Raja Motors not found. Creating new Owner...");
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
                    "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab"
            );
            ownerRepository.save(rajaOwner);
            seedRajaMotorsBranchesAndData(rajaOwner);
            System.out.println(">>> Raja Motors created and seeded successfully <<<");
        } else {
            System.out.println("[DEBUG] Raja Motors user/email already exists. Syncing Owner details...");
            Optional<User> existingUser = userRepository.findById(rajaId);
            if (existingUser.isPresent()) {
                Optional<Owner> existingOwner = ownerRepository.findById(rajaId);
                Owner owner;
                if (existingOwner.isPresent()) {
                    owner = existingOwner.get();
                    System.out.println("[DEBUG] Existing Owner record found.");
                } else {
                    System.out.println("[DEBUG] User exists but Owner record missing. Creating Owner record...");
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
                
                // Sync essential details for analytics
                owner.setOwnerCode("RAJA001");
                owner.setCompanyName("Raja Motors");
                ownerRepository.save(owner);
                
                System.out.println("[DEBUG] Proceeding to seed branches and history for: " + owner.getEmail());
                seedRajaMotorsBranchesAndData(owner);
            } else {
                System.out.println("[CRITICAL] User ID " + rajaId + " was expected but not found in userRepository!");
            }
            System.out.println(">>> Raja Motors check complete <<<");
        }
    }

    private void seedRajaMotorsBranchesAndData(Owner owner) {
        try {
            int existingCount = serviceCenterRepository.findByOwner_UserId(owner.getUserId()).size();
            long historyCount = bookingRepository.countByTenantId(owner.getUserId());
            
            System.out.println("[DEBUG] Seeding Raja Motors - Branches: " + existingCount + ", History: " + historyCount);

            // SKIP SEEDING if history already exists to save time on startup
            if (historyCount > 0) {
                System.out.println("[DEBUG] Raja Motors already has history data. Skipping re-seed to speed up startup.");
                return;
            }

            System.out.println("[DEBUG] NO HISTORY FOUND: Seeding history for Raja Motors...");

        System.out.println("Seeding 3 branches for Raja Motors...");
        // Delete existing ones to start fresh with 3 branches if it was partially seeded
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
                System.out.println("Cleaned up incomplete centers for re-seed.");
            }
        }

        String[] locations = {"Colombo", "Kandy", "Galle"};
        List<ServiceCenter> centers = new ArrayList<>();
        List<ServicePackage> packages = new ArrayList<>();
        List<Manager> managers = new ArrayList<>();

        if (serviceCenterRepository.findByOwner_UserId(owner.getUserId()).size() < 3) {
            for (String loc : locations) {
                ServiceCenter sc = new ServiceCenter(UUID.randomUUID(), owner, "Raja Motors - " + loc, loc,
                        "+94112000" + loc.length(), "08:00 - 18:00", new BigDecimal("4.5"), true, LocalDateTime.now(), "system",
                        LocalDateTime.now(), "system", new String[] {"Toyota", "Honda", "Nissan", "Suzuki"}, "APPROVED", null, null, null, null, null);
                centers.add(serviceCenterRepository.save(sc));

                ServicePackage sp = new ServicePackage(UUID.randomUUID(), sc, "Premium Full Service", "Full maintenance package", 
                        "Oil change, filter, brake check, etc.", new BigDecimal("12500.00"), 120, true, 
                        LocalDateTime.now(), "system", LocalDateTime.now(), "system");
                packages.add(servicePackageRepository.save(sp));

                Manager mgr = new Manager(UUID.randomUUID(), loc + " Manager", "manager." + loc.toLowerCase() + "@raja.lk", 
                        "+94771000" + loc.length(), passwordEncoder.encode("manager123"), "ROLE_SERVICE_MANAGER", true, 
                        null, LocalDateTime.now(), "system", LocalDateTime.now(), "system", 
                        "https://i.pravatar.cc/150", "MGR-" + loc.substring(0, 3).toUpperCase(), sc.getCenterId());
                managers.add(managerRepository.save(mgr));
            }
        } else {
            centers = serviceCenterRepository.findByOwner_UserId(owner.getUserId());
            // Safe mapping: Ensure we have at least one package per center for seeding
            for (ServiceCenter center : centers) {
                List<ServicePackage> centerPackages = servicePackageRepository.findByServiceCenter_CenterIdAndIsActiveTrue(center.getCenterId());
                if (centerPackages.isEmpty()) {
                    ServicePackage sp = new ServicePackage(UUID.randomUUID(), center, "Standard Service", "Base maintenance", 
                            "Essential checks and oil service.", new BigDecimal("8500.00"), 60, true, 
                            LocalDateTime.now(), "system", LocalDateTime.now(), "system");
                    packages.add(servicePackageRepository.save(sp));
                } else {
                    packages.add(centerPackages.get(0));
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
            int dailyBookings = 2 + (int)(Math.random() * 3);
            for (int i = 0; i < dailyBookings; i++) {
                ServiceCenter center = centers.get((int)(Math.random() * centers.size()));
                Customer customer = customers.get((int)(Math.random() * customers.size()));
                ServicePackage pkg = packages.get(centers.indexOf(center));
                
                Booking b = new Booking();
                b.setBookingId(UUID.randomUUID());
                b.setTenantId(owner.getUserId());
                b.setCenterId(center.getCenterId());
                b.setCustomerId(customer.getUserId());
                b.setVehicleId(UUID.randomUUID());
                b.setPackageId(pkg.getPackageId());
                b.setBookingDate(bookingDateTime.toLocalDate());
                b.setBookingTime(LocalTime.of(9 + (int)(Math.random() * 8), 0));
                
                // Determine status based on age
                if (day > 2) {
                    // Older than 2 days -> mostly COMPLETED
                    b.setStatus(com.fixzone.fixzon_backend.enums.BookingStatus.COMPLETED);
                } else if (day == 0) {
                    // Today -> mostly CONFIRMED or IN_PROGRESS
                    b.setStatus(Math.random() > 0.5 ? com.fixzone.fixzon_backend.enums.BookingStatus.CONFIRMED : com.fixzone.fixzon_backend.enums.BookingStatus.IN_PROGRESS);
                } else {
                    b.setStatus(com.fixzone.fixzon_backend.enums.BookingStatus.COMPLETED);
                }
                
                b.setEstimatedCost(pkg.getBasePrice());
                b.setBookingFee(new BigDecimal("1000.00"));
                b.setBookingFeePaid(true);
                b.setCreatedAt(bookingDateTime);
                
                bookingRepository.save(b);

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
                inv.setStatus(b.getStatus() == com.fixzone.fixzon_backend.enums.BookingStatus.COMPLETED ? "PAID" : "PENDING");
                inv.setIssuedAt(bookingDateTime.plusHours(2));
                inv.setCreatedAt(bookingDateTime.plusHours(2));
                invoiceRepository.save(inv);

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
        System.out.println("[SUCCESS] Raja Motors seeding completed successfully for " + owner.getEmail());
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to seed Raja Motors data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
