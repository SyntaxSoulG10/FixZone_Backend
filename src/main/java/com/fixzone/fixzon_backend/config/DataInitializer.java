package com.fixzone.fixzon_backend.config;

import com.fixzone.fixzon_backend.model.*;
import com.fixzone.fixzon_backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
//import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import com.fixzone.fixzon_backend.enums.BookingStatus;
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
    private final BookingHistoryRepository bookingHistoryRepository;
    private final PaymentRepository paymentRepository;
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
                           BookingHistoryRepository bookingHistoryRepository,
                           PaymentRepository paymentRepository,
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
        this.bookingHistoryRepository = bookingHistoryRepository;
        this.paymentRepository = paymentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // If we already have 20 users (our target), skip initialization

        if (userRepository.count() >= 20) {
            System.out.println("Data already exists (20+ users), skipping initialization.");
            return;
        }

        System.out.println("--- CLEARING OLD DATA AND STARTING FRESH SRI LANKAN SEEDING ---");

        // Deletion in order to respect foreign key constraints
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

        // Delete child users before parent users
        managerRepository.deleteAll();
        customerRepository.deleteAll();
        ownerRepository.deleteAll();
        superAdminRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Super Admins (5)
        List<SuperAdmin> superAdmins = new ArrayList<>();
        String[] adminNames = {"Aruna Kumara", "Ruwan Silva", "Gihan Fernando", "Amila Jayasinghe", "Suraj De Silva"};
        for (int i = 0; i < 5; i++) {
            superAdmins.add(new SuperAdmin(UUID.randomUUID(), adminNames[i], "admin" + (i + 1) + "@fixzone.lk", "+9411555000" + i, passwordEncoder.encode("Admin123!"), "ROLE_SUPER_ADMIN", true, LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "https://i.pravatar.cc/150?u=" + adminNames[i].replace(" ", "+"), "ADM-00" + (i + 1)));
        }
        superAdminRepository.saveAll(superAdmins);

        // 2. Owners (5)
        List<Owner> owners = new ArrayList<>();
        String[] ownerNames = {"Janaka Ranasinghe", "Tharindu Perera", "Eranga Fernando", "Piyal Sirisena", "Malith Gunawardena"};
        String[] companies = {"Ranasinghe Motors", "Perera Auto", "Fernando Repairs", "Sirisena Garage", "Gunawardena Services"};
        for (int i = 0; i < 5; i++) {
            owners.add(new Owner(UUID.randomUUID(), ownerNames[i], "owner" + (i + 1) + "@fixzone.lk", "+9477100000" + i, passwordEncoder.encode("pass123"), "OWNER", true, LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "https://i.pravatar.cc/150?u=" + ownerNames[i].replace(" ", "+"), "FIX00" + (i + 1), companies[i], "contact@" + ownerNames[i].toLowerCase().replace(" ", "") + ".lk", "+9411200000" + i));
        }
        ownerRepository.saveAll(owners);

        // 3. Customers (5)
        List<Customer> customers = new ArrayList<>();
        String[] customerNames = {"Lakmal Senaratne", "Indika Bandara", "Saman Kumara", "Upul Kumara", "Ranjan Perera"};
        for (int i = 0; i < 5; i++) {
            customers.add(new Customer(UUID.randomUUID(), customerNames[i], "customer" + (i + 1) + "@fixzone.lk", "+9477300000" + i, passwordEncoder.encode("pass123"), "CUSTOMER", true, LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "https://i.pravatar.cc/150?u=" + customerNames[i].replace(" ", "+"), "CUST-00" + (i + 1), "EMAIL"));
        }
        customerRepository.saveAll(customers);

        // 4. Service Centers (3 per owner = 15)
        List<ServiceCenter> centers = new ArrayList<>();
        String[][] branchLocations = {
                {"Colombo", "Kandy", "Galle"},
                {"Negombo", "Jaffna", "Matara"},
                {"Kurunegala", "Anuradhapura", "Ratnapura"},
                {"Kalutara", "Gampaha", "Trincomalee"},
                {"Badulla", "Nuwara Eliya", "Batticaloa"}
        };
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 3; j++) {
                String branchName = owners.get(i).getCompanyName() + "-" + branchLocations[i][j];
                String phone = "+9411400" + i + j;
                centers.add(new ServiceCenter(UUID.randomUUID(), owners.get(i), branchName, branchLocations[i][j], phone, "08:00 - 18:00", new BigDecimal("4." + (5 + i)), true, LocalDateTime.now(), "system", LocalDateTime.now(), "system", new String[]{"Toyota", "Nissan", "Suzuki"}, "APPROVED", null, null, null, null, null));
            }
        }
        serviceCenterRepository.saveAll(centers);

        // 5. Managers (15 - one per branch)
        List<Manager> managers = new ArrayList<>();
        for (int i = 0; i < centers.size(); i++) {
            ServiceCenter center = centers.get(i);
            String mgrName = "Manager " + (i + 1);
            String mgrEmail = "manager" + (i + 1) + "@fixzone.lk";
            managers.add(new Manager(UUID.randomUUID(), mgrName, mgrEmail, "+94772000" + i, passwordEncoder.encode("pass123"), "MANAGER", true, LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "https://i.pravatar.cc/150?u=mgr" + i, "MGR-00" + (i + 1), center.getCenterId()));
        }
        managerRepository.saveAll(managers);

        // 6. Service Packages
        List<ServicePackage> packages = new ArrayList<>();
        String[] pkgNames = {"Full Service", "Brake Check", "Hybrid Diagnostics", "Interior Detailing", "Nano Coating"};
        BigDecimal[] prices = {new BigDecimal("18000.00"), new BigDecimal("4500.00"), new BigDecimal("12000.00"), new BigDecimal("7500.00"), new BigDecimal("45000.00")};
        for (ServiceCenter center : centers) {
            for (int i = 0; i < pkgNames.length; i++) {
                packages.add(new ServicePackage(UUID.randomUUID(), center, pkgNames[i] + " (" + center.getAddress() + ")", "Package", "Premium " + pkgNames[i] + " in " + center.getAddress(), prices[i], 60 + (i * 30), true, LocalDateTime.now(), "system", LocalDateTime.now(), "system"));
            }
        }
        servicePackageRepository.saveAll(packages);

        // 7. Bookings
       List<Booking> bookings = new ArrayList<>();

        for (int i = 0; i < 10; i++) {

        ServiceCenter center = centers.get(i % centers.size());
        Customer customer = customers.get(i % customers.size());

        ServicePackage pkg = packages.stream()
        .filter(p -> p.getServiceCenter().getCenterId().equals(center.getCenterId()))
        .findFirst()
        .orElse(null);

        Booking b = new Booking();

    
        // CORE DATA
   
        b.setTenantId(center.getOwner().getUserId());
        b.setCenterId(center.getCenterId());
        b.setCustomerId(customer.getUserId());
        b.setVehicleId(UUID.randomUUID());

    if (pkg != null) {
        b.setPackageId(pkg.getPackageId());
        b.setEstimatedCost(pkg.getBasePrice());

        // 🔥 match service logic (10% fee from service)
        b.setBookingFee(pkg.getBasePrice()
            .multiply(BigDecimal.valueOf(0.10)));
    }

    b.setBookingDate(LocalDate.now().plusDays(i + 1));
    b.setBookingTime(LocalTime.of(9 + (i % 5), 0));

    // STATUS FLOW (REALISTIC)
   
    switch (i) {

        //  Just created
        case 0, 1:
            b.setStatus(BookingStatus.PENDING_PAYMENT);
            break;

        //  Paid → CONFIRMED
        case 2, 3:
            b.setStatus(BookingStatus.CONFIRMED);
            b.setBookingFeePaid(true);
            b.setStripePaymentId(UUID.randomUUID().toString());
            break;

        //  Completed
        case 4, 5:
            b.setStatus(BookingStatus.COMPLETED);
            b.setBookingFeePaid(true);
            b.setStripePaymentId(UUID.randomUUID().toString());
            break;

        // Cancelled
        case 6, 7:
            b.setStatus(BookingStatus.CANCELLED);
            b.setIsCancelled(true);
            b.setCancelledAt(LocalDateTime.now());

            //  match service penalty logic (5%)
            b.setCancellationPenalty(
                b.getEstimatedCost().multiply(BigDecimal.valueOf(0.05))
            );
            break;

        //  Rescheduled cases
        default:
            b.setStatus(BookingStatus.CONFIRMED);
            b.setBookingFeePaid(true);
            b.setStripePaymentId(UUID.randomUUID().toString());
            b.setRescheduleCount(1);
            break;
    }

    b.setSpecialRequest("Seeded booking");

    bookings.add(b);
}

bookingRepository.saveAll(bookings);

    
        // 7. & 8. Bookings & Invoices (Historical Data for Analytics)
        System.out.println("Seeding historical data for all branches...");
        List<Booking> historicalBookings = new ArrayList<>();
        List<Invoice> invoices = new ArrayList<>();
        List<PaymentRecord> payments = new ArrayList<>();

        // Months to seed: Jan (1), Feb (2), Mar (3), Apr (4)
        int[] months = {1, 2, 3, 4};

        // Seed data for EVERY branch
        for (int c = 0; c < centers.size(); c++) {
            ServiceCenter center = centers.get(c);
            Owner owner = (Owner) center.getOwner();

            // Vary booking counts by month and by branch to make analytics look real
            int baseCount = 10 + (c % 5);
            int[] bookingCounts = {baseCount, baseCount + 5, baseCount + 10, baseCount + 15};

            // Find packages for this specific center
            List<ServicePackage> centerPackages = packages.stream()
                    .filter(p -> p.getServiceCenter().getCenterId().equals(center.getCenterId()))
                    .toList();

            if (centerPackages.isEmpty()) continue;

            for (int m = 0; m < months.length; m++) {
                for (int i = 0; i < bookingCounts[m]; i++) {
                    LocalDateTime date = LocalDateTime.of(2026, months[m], (i % 28) + 1, 9 + (i % 8), 0);

                    Booking b = new Booking();
                    // b.setBookingId(UUID.randomUUID());  in model auto doing this
                    // b.setCenterId(center.getCenterId());
                    // b.setTenantId(owner.getUserId());
                    //  b.setCustomerId(customers.get(i % 5).getUserId());
                    b.setVehicleId(UUID.randomUUID());
                    b.setPackageId(centerPackages.get(i % centerPackages.size()).getPackageId());
                    // b.setPreferredDateTime(date.plusDays(1));
                    b.setStatus(i % 12 == 0 ? BookingStatus.CONFIRMED : BookingStatus.COMPLETED);
                    // b.setPriority("NORMAL");
                    b.setEstimatedCost(centerPackages.get(i % centerPackages.size()).getBasePrice());
                    b.setCreatedAt(date);
                    historicalBookings.add(b);
                    b.setBookingDate(date.toLocalDate()); //add by me
                    b.setBookingTime(date.toLocalTime()); // add by me

                    if (b.getStatus().equals(BookingStatus.CONFIRMED)) {
                        UUID invId = UUID.randomUUID();
                        BigDecimal total = b.getEstimatedCost();
                        // Simpler calculation: no breakdown in invoice per previous discussion
                        Invoice inv = new Invoice(invId, owner.getOwnerCode(), center.getCenterId(), b.getBookingId(), b.getCustomerId(), total, BigDecimal.ZERO, BigDecimal.ZERO, total, "PAID", date.plusHours(2), date.plusDays(1), date, "system", date, "system");
                        invoices.add(inv);

                        String method = i % 3 == 0 ? "CASH" : "CARD"; // Mix of CASH (Hand Collection) and CARD (Online)
                        payments.add(new PaymentRecord(UUID.randomUUID(), invId, center.getCenterId(), total, method, "TXN-" + c + "-" + months[m] + "-" + i, "Completed", date.plusHours(2), date.plusHours(2), "system", date.plusHours(2), "system"));
                    }
                }
            }
        }

        bookingRepository.saveAll(historicalBookings);
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
        Owner pendingOwner = new Owner(UUID.randomUUID(), "Kusal Mendis", "kusal@test.com", "+94775000000", passwordEncoder.encode("pass123"), "OWNER", true, LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "https://i.pravatar.cc/150?u=Kusal+Mendis", "FIXPEND01", "Mendis Auto", "contact@mendis.lk", "+94112555555");
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
        pendingCenter.setSupportedVehicleBrands(new String[]{"Toyota", "Honda"});
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
        pendingCenter2.setSupportedVehicleBrands(new String[]{"Mitsubishi", "Suzuki"});
        serviceCenterRepository.save(pendingCenter2);

        System.out.println("--- SRI LANKAN DATA SEEDING COMPLETE WITH PENDING REQUESTS ---");
    }
}
