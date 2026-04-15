package com.fixzone.fixzon_backend.config;

import com.fixzone.fixzon_backend.entity.*;
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
        // If we have less than 20 users (our target), let's clear and re-seed to avoid partial data conflicts
        if (userRepository.count() >= 20) {
            System.out.println("Data already exists (20+ users), skipping initialization.");
            return;
        }

        System.out.println("--- CLEARING OLD DATA AND STARTING FRESH SEEDING ---");
        
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

        System.out.println("--- STARTING REFINED SRI LANKAN DATA SEEDING (NON-CRICKETERS) ---");

        // 1. Super Admins (5)
        List<SuperAdmin> superAdmins = new ArrayList<>();
        String[] adminNames = {"Aruna Kumara", "Ruwan Silva", "Gihan Fernando", "Amila Jayasinghe", "Suraj De Silva"};
        for (int i = 0; i < 5; i++) {
            superAdmins.add(new SuperAdmin(UUID.randomUUID(), adminNames[i], "admin" + (i + 1) + "@fixzone.lk", "+9411555000" + i, passwordEncoder.encode("Admin123!"), "ROLE_SUPER_ADMIN", true, LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "ADM-00" + (i + 1)));
        }
        superAdminRepository.saveAll(superAdmins);

        // 2. Owners (5)
        List<Owner> owners = new ArrayList<>();
        String[] ownerNames = {"Janaka Ranasinghe", "Tharindu Perera", "Eranga Fernando", "Piyal Sirisena", "Malith Gunawardena"};
        String[] companies = {"Ranasinghe Motors", "Perera Auto", "Fernando Repairs", "Sirisena Garage", "Gunawardena Services"};
        for (int i = 0; i < 5; i++) {
            owners.add(new Owner(UUID.randomUUID(), ownerNames[i], "owner" + (i + 1) + "@fixzone.lk", "+9477100000" + i, passwordEncoder.encode("pass123"), "OWNER", true, LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "OWN-00" + (i + 1), companies[i], "contact@" + ownerNames[i].toLowerCase().replace(" ", "") + ".lk", "+9411200000" + i));
        }
        ownerRepository.saveAll(owners);

        // 3. Customers (5)
        List<Customer> customers = new ArrayList<>();
        String[] customerNames = {"Lakmal Senaratne", "Indika Bandara", "Saman Kumara", "Upul Kumara", "Ranjan Perera"};
        for (int i = 0; i < 5; i++) {
            customers.add(new Customer(UUID.randomUUID(), customerNames[i], "customer" + (i + 1) + "@fixzone.lk", "+9477300000" + i, passwordEncoder.encode("pass123"), "CUSTOMER", true, LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "CUST-00" + (i + 1), "EMAIL"));
        }
        customerRepository.saveAll(customers);

        // 4. Service Centers (5)
        List<ServiceCenter> centers = new ArrayList<>();
        String[] locations = {"Colombo 03", "Kandy Town", "Galle Fort", "Jaffna Central", "Negombo Coastal"};
        for (int i = 0; i < 5; i++) {
            centers.add(new ServiceCenter(UUID.randomUUID(), owners.get(i), companies[i] + " HQ", locations[i], "+9411400000" + i, "08:00 - 18:00", new BigDecimal("4." + (5 + i)), true, LocalDateTime.now(), "system", LocalDateTime.now(), "system", new String[]{"Toyota", "Nissan", "Suzuki"}, "APPROVED"));
        }
        serviceCenterRepository.saveAll(centers);

        // 5. Managers (5) - Now linked to centers
        List<Manager> managers = new ArrayList<>();
        String[] managerNames = {"Roshan Wijesinghe", "Hasitha Abeyratne", "Mahesh Amarasinghe", "Prabath Rathnayake", "Vishwa Kumara"};
        for (int i = 0; i < 5; i++) {
            managers.add(new Manager(UUID.randomUUID(), managerNames[i], "manager" + (i + 1) + "@fixzone.lk", "+9477200000" + i, passwordEncoder.encode("pass123"), "MANAGER", true, LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "MGR-00" + (i + 1), centers.get(i).getCenterId()));
        }
        managerRepository.saveAll(managers);

        // 6. Service Packages (5 per center)
        List<ServicePackage> packages = new ArrayList<>();
        String[] pkgNames = {"Full Service", "Brake Check", "Hybrid Diagnostics", "Interior Detailing", "Nano Coating"};
        BigDecimal[] prices = {new BigDecimal("18000.00"), new BigDecimal("4500.00"), new BigDecimal("12000.00"), new BigDecimal("7500.00"), new BigDecimal("45000.00")};
        for (ServiceCenter center : centers) {
            for (int i = 0; i < pkgNames.length; i++) {
                packages.add(new ServicePackage(UUID.randomUUID(), center.getCenterId(), pkgNames[i] + " (" + center.getAddress() + ")", "Package", "Premium " + pkgNames[i] + " in " + center.getAddress(), prices[i], 60 + (i * 30), true, LocalDateTime.now(), "system", LocalDateTime.now(), "system"));
            }
        }
        servicePackageRepository.saveAll(packages);

        // 7. Bookings
        List<Booking> bookings = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Booking b = new Booking();
            b.setBookingId(UUID.randomUUID());
            b.setCenterId(centers.get(i).getCenterId());
            b.setTenantId(owners.get(i).getUserId()); 
            b.setCustomerId(customers.get(i).getUserId());
            b.setVehicleId(UUID.randomUUID());
            b.setPackageId(packages.get(i).getPackageId());
            b.setPreferredDateTime(LocalDateTime.now().plusDays(i + 1));
            b.setStatus("PENDING");
            b.setPriority("NORMAL");
            b.setEstimatedCost(packages.get(i).getBasePrice());
            bookings.add(b);
        }
        bookingRepository.saveAll(bookings);

        // 8. Invoices & Payments
        List<Invoice> invoices = new ArrayList<>();
        List<PaymentRecord> payments = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            UUID invId = UUID.randomUUID();
            BigDecimal total = packages.get(i).getBasePrice();
            BigDecimal tax = total.multiply(new BigDecimal("0.1")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtot = total.subtract(tax);
            Invoice inv = new Invoice(invId, "INV-GEN-" + (1000 + i), centers.get(i).getCenterId(), bookings.get(i).getBookingId(), customers.get(i).getUserId(), subtot, tax, BigDecimal.ZERO, total, "PAID", LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system");
            invoices.add(inv);
            payments.add(new PaymentRecord(UUID.randomUUID(), invId, centers.get(i).getCenterId(), total, "ONLINE_BANKING", "TXN-GEN-" + i, "Completed", LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system"));
        }
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

        System.out.println("--- REFINED SRI LANKAN DATA SEEDING COMPLETE ---");
    }
}
