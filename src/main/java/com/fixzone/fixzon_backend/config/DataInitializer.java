package com.fixzone.fixzon_backend.config;

import com.fixzone.fixzon_backend.model.*;
import com.fixzone.fixzon_backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final OwnerRepository ownerRepository;
    private final CustomerRepository customerRepository;
    private final ManagerRepository managerRepository;
    private final SuperAdminRepository superAdminRepository;
    private final ServiceCenterRepository serviceCenterRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final BookingRepository bookingRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final NotificationRepository notificationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AnalyticsRepository analyticsRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
            OwnerRepository ownerRepository,
            CustomerRepository customerRepository,
            ManagerRepository managerRepository,
            SuperAdminRepository superAdminRepository,
            ServiceCenterRepository serviceCenterRepository,
            ServicePackageRepository servicePackageRepository,
            BookingRepository bookingRepository,
            InvoiceRepository invoiceRepository,
            PaymentRecordRepository paymentRecordRepository,
            NotificationRepository notificationRepository,
            SubscriptionRepository subscriptionRepository,
            AnalyticsRepository analyticsRepository,
            PasswordEncoder passwordEncoder) {
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
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // If we already have 20 users (our target), skip initialization
        /*
         * if (userRepository.count() >= 20) {
         * System.out.
         * println("Data already exists (20+ users), skipping initialization.");
         * return;
         * }
         */

        System.out.println("--- CLEARING OLD DATA AND STARTING FRESH SRI LANKAN SEEDING ---");

        // Deletion in order to respect foreign key constraints
        analyticsRepository.deleteAll();
        subscriptionRepository.deleteAll();
        notificationRepository.deleteAll();
        paymentRecordRepository.deleteAll();
        invoiceRepository.deleteAll();
        bookingRepository.deleteAll();
        servicePackageRepository.deleteAll();
        serviceCenterRepository.deleteAll();

        // Delete child users before parent users
        managerRepository.deleteAll();
        customerRepository.deleteAll();
        ownerRepository.deleteAll();
        superAdminRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Super Admins (5)
        List<SuperAdmin> superAdmins = new ArrayList<>();
        String[] adminNames = { "Aruna Kumara", "Ruwan Silva", "Gihan Fernando", "Amila Jayasinghe", "Suraj De Silva" };
        for (int i = 0; i < 5; i++) {
            superAdmins.add(new SuperAdmin(UUID.randomUUID(), adminNames[i], "admin" + (i + 1) + "@fixzone.lk",
                    "+9411555000" + i, passwordEncoder.encode("Admin123!"), "ROLE_SUPER_ADMIN", true,
                    LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system",
                    "https://i.pravatar.cc/150?u=" + adminNames[i].replace(" ", "+"), "ADM-00" + (i + 1)));
        }
        superAdminRepository.saveAll(superAdmins);

        // 2. Owners (5)
        List<Owner> owners = new ArrayList<>();
        String[] ownerNames = { "Janaka Ranasinghe", "Tharindu Perera", "Eranga Fernando", "Piyal Sirisena",
                "Malith Gunawardena" };
        String[] companies = { "Ranasinghe Motors", "Perera Auto", "Fernando Repairs", "Sirisena Garage",
                "Gunawardena Services" };
        for (int i = 0; i < 5; i++) {
            owners.add(new Owner(UUID.randomUUID(), ownerNames[i], "owner" + (i + 1) + "@fixzone.lk", "+9477100000" + i,
                    passwordEncoder.encode("pass123"), "OWNER", true, LocalDateTime.now(), LocalDateTime.now(),
                    "system", LocalDateTime.now(), "system",
                    "https://i.pravatar.cc/150?u=" + ownerNames[i].replace(" ", "+"), "FIX00" + (i + 1), companies[i],
                    "contact@" + ownerNames[i].toLowerCase().replace(" ", "") + ".lk", "+9411200000" + i));
        }
        ownerRepository.saveAll(owners);

        // 3. Customers (5)
        List<Customer> customers = new ArrayList<>();
        String[] customerNames = { "Lakmal Senaratne", "Indika Bandara", "Saman Kumara", "Upul Kumara",
                "Ranjan Perera" };
        for (int i = 0; i < 5; i++) {
            customers.add(new Customer(UUID.randomUUID(), customerNames[i], "customer" + (i + 1) + "@fixzone.lk",
                    "+9477300000" + i, passwordEncoder.encode("pass123"), "CUSTOMER", true, LocalDateTime.now(),
                    LocalDateTime.now(), "system", LocalDateTime.now(), "system",
                    "https://i.pravatar.cc/150?u=" + customerNames[i].replace(" ", "+"), "CUST-00" + (i + 1), "EMAIL"));
        }
        customerRepository.saveAll(customers);

        // 4. Service Centers (5)
        List<ServiceCenter> centers = new ArrayList<>();
        String[] locations = { "Colombo 03", "Kandy Town", "Galle Fort", "Jaffna Central", "Negombo Coastal" };
        for (int i = 0; i < 5; i++) {
            // Note: Added null for servicePackages list argument
            centers.add(new ServiceCenter(UUID.randomUUID(), owners.get(i), companies[i] + " HQ", locations[i],
                    "+9411400000" + i, "08:00 - 18:00", new BigDecimal("4." + (5 + i)), true, LocalDateTime.now(),
                    "system", LocalDateTime.now(), "system", new String[] { "Toyota", "Nissan", "Suzuki" }, "APPROVED",
                    null, null, null, null, null));
        }
        serviceCenterRepository.saveAll(centers);

        // 5. Managers (5)
        List<Manager> managers = new ArrayList<>();
        String[] managerNames = { "Roshan Wijesinghe", "Hasitha Abeyratne", "Maheshi Amarasinghe", "Praba Rathnayake",
                "Vishwa Kumara" };
        for (int i = 0; i < 5; i++) {
            managers.add(new Manager(UUID.randomUUID(), managerNames[i], "manager" + (i + 1) + "@fixzone.lk",
                    "+9477200000" + i, passwordEncoder.encode("pass123"), "MANAGER", true, LocalDateTime.now(),
                    LocalDateTime.now(), "system", LocalDateTime.now(), "system",
                    "https://i.pravatar.cc/150?u=" + managerNames[i].replace(" ", "+"), "MGR-00" + (i + 1),
                    centers.get(i).getCenterId()));
        }
        managerRepository.saveAll(managers);

        // 6. Service Packages
        List<ServicePackage> packages = new ArrayList<>();
        String[] pkgNames = { "Full Service", "Brake Check", "Hybrid Diagnostics", "Interior Detailing",
                "Nano Coating" };
        BigDecimal[] prices = { new BigDecimal("18000.00"), new BigDecimal("4500.00"), new BigDecimal("12000.00"),
                new BigDecimal("7500.00"), new BigDecimal("45000.00") };
        for (ServiceCenter center : centers) {
            for (int i = 0; i < pkgNames.length; i++) {
                // Note: centerId replaced with ServiceCenter object
                packages.add(
                        new ServicePackage(UUID.randomUUID(), center, pkgNames[i] + " (" + center.getAddress() + ")",
                                "Package", "Premium " + pkgNames[i] + " in " + center.getAddress(), prices[i],
                                60 + (i * 30), true, LocalDateTime.now(), "system", LocalDateTime.now(), "system"));
            }
        }
        servicePackageRepository.saveAll(packages);

        // 7. & 8. Bookings & Invoices (Historical Data for Analytics)
        System.out.println("Seeding historical data for FIX001...");
        List<Booking> bookings = new ArrayList<>();
        List<Invoice> invoices = new ArrayList<>();
        List<PaymentRecord> payments = new ArrayList<>();

        Owner mainOwner = owners.get(0); // FIX-001
        ServiceCenter mainCenter = centers.get(0);

        // Months to seed: Jan, Feb, Mar, Apr (current)
        int[] bookingCounts = { 15, 22, 28, 35 };
        int[] months = { 1, 2, 3, 4 };

        for (int m = 0; m < months.length; m++) {
            for (int i = 0; i < bookingCounts[m]; i++) {
                LocalDateTime date = LocalDateTime.of(2026, months[m], (i % 28) + 1, 10, 0);

                Booking b = new Booking();
                b.setBookingId(UUID.randomUUID());
                b.setCenterId(mainCenter.getCenterId());
                b.setTenantId(mainOwner.getUserId());
                b.setCustomerId(customers.get(i % 5).getUserId());
                b.setVehicleId(UUID.randomUUID());
                b.setPackageId(packages.get(i % 5).getPackageId());
                b.setBookingDate(date.toLocalDate().plusDays(1));
                b.setBookingTime(date.toLocalTime());
                b.setStatus(i % 10 == 0 ? com.fixzone.fixzon_backend.enums.BookingStatus.PENDING_PAYMENT
                        : com.fixzone.fixzon_backend.enums.BookingStatus.COMPLETED);
                b.setEstimatedCost(packages.get(i % 5).getBasePrice());
                b.setCreatedAt(date);
                bookings.add(b);

                if (b.getStatus() != com.fixzone.fixzon_backend.enums.BookingStatus.PENDING_PAYMENT) {
                    UUID invId = UUID.randomUUID();
                    BigDecimal total = b.getEstimatedCost();
                    BigDecimal tax = total.multiply(new BigDecimal("0.1")).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal subtot = total.subtract(tax);

                    Invoice inv = new Invoice(invId, mainOwner.getOwnerCode(), mainCenter.getCenterId(),
                            b.getBookingId(), b.getCustomerId(), subtot, tax, BigDecimal.ZERO, total, "PAID",
                            date.plusHours(2), date.plusDays(1), date, "system", date, "system");
                    invoices.add(inv);

                    payments.add(new PaymentRecord(UUID.randomUUID(), invId, mainCenter.getCenterId(), total, "CASH",
                            "TXN-" + months[m] + "-" + i, "Completed", date.plusHours(2), date.plusHours(2), "system",
                            date.plusHours(2), "system"));
                }
            }
        }

        // Add some data for other owners too
        for (int i = 1; i < 5; i++) {
            Booking b = new Booking();
            b.setBookingId(UUID.randomUUID());
            b.setCenterId(centers.get(i).getCenterId());
            b.setTenantId(owners.get(i).getUserId());
            b.setCustomerId(customers.get(i).getUserId());
            b.setVehicleId(UUID.randomUUID());
            b.setPackageId(packages.get(i).getPackageId());
            b.setBookingDate(LocalDate.now().plusDays(1));
            b.setBookingTime(LocalTime.now());
            b.setStatus(com.fixzone.fixzon_backend.enums.BookingStatus.PENDING_PAYMENT);
            b.setEstimatedCost(packages.get(i).getBasePrice());
            b.setCreatedAt(LocalDateTime.now());
            bookings.add(b);
        }

        bookingRepository.saveAll(bookings);
        invoiceRepository.saveAll(invoices);
        paymentRecordRepository.saveAll(payments);

        // 9. Notifications
        List<Notification> notifications = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Notification n = new Notification();
            n.setRecipient(customers.get(i));
            n.setTitle("Booking Update");
            n.setMessage("Reminder: Your service in " + centers.get(i).getAddress() + " is tomorrow.");
            n.setType("INFO");
            notifications.add(n);
        }
        notificationRepository.saveAll(notifications);

        // 10. Subscriptions
        List<Subscription> subscriptions = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Subscription s = new Subscription();
            s.setOwner(owners.get(i));
            s.setPlanType(i % 2 == 0 ? "Gold Plan" : "Enterprise Plan");
            s.setStartDate(LocalDate.now().minusMonths(1));
            s.setEndDate(LocalDate.now().plusMonths(11));
            s.setStatus("ACTIVE");
            subscriptions.add(s);
        }
        subscriptionRepository.saveAll(subscriptions);

        // 11. Analytics
        List<Analytics> analyticsRecords = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Analytics a = new Analytics();
            a.setServiceCenter(centers.get(i));
            a.setDate(LocalDate.now());
            a.setMetrics("Daily Active Users: " + (50 + (i * 10)));
            analyticsRecords.add(a);
        }
        analyticsRepository.saveAll(analyticsRecords);

        // --- NEW PENDING REGISTRATION REQUESTS FOR TASK 1 ---
        System.out.println("Adding PENDING registration requests for testing...");

        // Create a Pending Owner
        Owner pendingOwner = new Owner(UUID.randomUUID(), "Kusal Mendis", "kusal@test.com", "+94775000000",
                passwordEncoder.encode("pass123"), "OWNER", true, LocalDateTime.now(), LocalDateTime.now(), "system",
                LocalDateTime.now(), "system", "https://i.pravatar.cc/150?u=Kusal+Mendis", "FIXPEND01", "Mendis Auto",
                "contact@mendis.lk", "+94112555555");
        ownerRepository.save(pendingOwner);

        // Create a Pending Service Center with Documents
        ServiceCenter pendingCenter = new ServiceCenter();
        pendingCenter.setCenterId(UUID.randomUUID());
        pendingCenter.setOwner(pendingOwner);
        pendingCenter.setName("Kandy Auto Care");
        pendingCenter.setAddress("Dalada Vidiya, Kandy");
        pendingCenter.setContactPhone("+94812222222");
        pendingCenter.setStatus("PENDING");
        pendingCenter.setIsActive(false);
        pendingCenter.setBusinessRegUrl("https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf");
        pendingCenter.setNicUrl("https://ui-avatars.com/api/?name=NIC-Sample&background=random");
        pendingCenter.setSupportedVehicleBrands(new String[] { "Toyota", "Honda" });
        serviceCenterRepository.save(pendingCenter);

        // Create another Pending Service Center
        ServiceCenter pendingCenter2 = new ServiceCenter();
        pendingCenter2.setCenterId(UUID.randomUUID());
        pendingCenter2.setOwner(pendingOwner); // Same owner for simplicity
        pendingCenter2.setName("Galle Speed Works");
        pendingCenter2.setAddress("Marine Drive, Galle");
        pendingCenter2.setContactPhone("+94912222222");
        pendingCenter2.setStatus("PENDING");
        pendingCenter2.setIsActive(false);
        pendingCenter2.setBusinessRegUrl("https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf");
        pendingCenter2.setNicUrl("https://ui-avatars.com/api/?name=NIC-Sample&background=random");
        pendingCenter2.setSupportedVehicleBrands(new String[] { "Mitsubishi", "Suzuki" });
        serviceCenterRepository.save(pendingCenter2);

        System.out.println("--- SRI LANKAN DATA SEEDING COMPLETE WITH PENDING REQUESTS ---");
    }
}
