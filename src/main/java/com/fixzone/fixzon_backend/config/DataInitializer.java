package com.fixzone.fixzon_backend.config;

import com.fixzone.fixzon_backend.model.*;
import com.fixzone.fixzon_backend.repository.*;
import com.fixzone.fixzon_backend.enums.BookingStatus;
import com.fixzone.fixzon_backend.enums.BookingAction;
import com.fixzone.fixzon_backend.enums.PaymentStatus;
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
            UUID ownerId = (i == 0) ? UUID.fromString("00000000-0000-0000-0000-000000010011") : UUID.randomUUID();
            owners.add(new Owner(ownerId, ownerNames[i], "owner" + (i + 1) + "@fixzone.lk", "+9477100000" + i,
                    passwordEncoder.encode("pass123"), "OWNER", true, LocalDateTime.now(), LocalDateTime.now(),
                    "system", LocalDateTime.now(), "system",
                    "https://images.unsplash.com/photo-1560250097-0b93528c311a?q=80&w=200&h=200&auto=format&fit=crop", "FIX00" + (i + 1), companies[i],
                    "contact@" + ownerNames[i].toLowerCase().replace(" ", "") + ".lk", "+9411200000" + i));
        }
        ownerRepository.saveAll(owners);

        // 3. Customers (5)
        List<Customer> customers = new ArrayList<>();
        String[] customerNames = { "Lakmal Senaratne", "Indika Bandara", "Saman Kumara", "Upul Kumara",
                "Ranjan Perera" };
        for (int i = 0; i < 5; i++) {
            Customer c = new Customer(UUID.randomUUID(), customerNames[i], "customer" + (i + 1) + "@fixzone.lk",
                    "+9477300000" + i, passwordEncoder.encode("pass123"), "CUSTOMER", true, LocalDateTime.now(),
                    LocalDateTime.now(), "system", LocalDateTime.now(), "system",
                    "https://images.unsplash.com/photo-" + (new String[]{"1507003211169-0a1dd7228f2d", "1494790108377-be9c29b29330", "1500648767791-00dcc994a43e", "1599566150163-29194dcaad36", "1573497019940-1c28c88b4f3e"}[i]) + "?q=80&w=150&h=150&auto=format&fit=crop", "CUST-00" + (i + 1), "EMAIL");
            c.setVisits(5 + (i * 3));
            c.setTotalSpent(new BigDecimal(5000 + (i * 2500)));
            customers.add(c);
        }
        customerRepository.saveAll(customers);

        // 4. Service Centers (5 owners x 3 branches = 15 centers)
        List<ServiceCenter> centers = new ArrayList<>();
        String[][] branchLocations = {
                { "Bambalapitiya", "Kollupitiya", "Wellawatte" },
                { "Kandy Town", "Peradeniya", "Katugastota" },
                { "Galle Fort", "Karapitiya", "Hikkaduwa" },
                { "Matara Central", "Weligama", "Mirissa" },
                { "Negombo Coastal", "Kochchikade", "Ja-Ela" }
        };

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 3; j++) {
                String branchName = owners.get(i).getCompanyName() + " - " + branchLocations[i][j];
                centers.add(new ServiceCenter(UUID.randomUUID(), owners.get(i), branchName, branchLocations[i][j],
                        "+9411400" + i + j, "08:00 - 18:00", new BigDecimal("4." + (5 + i)), true, LocalDateTime.now(), "system",
                        LocalDateTime.now(), "system", new String[] { "Toyota", "Nissan", "Suzuki" }, "APPROVED", null,
                        null, null, null, null));
            }
        }
        serviceCenterRepository.saveAll(centers);

        // 5. Managers
        List<Manager> managers = new ArrayList<>();
        for (int i = 0; i < centers.size(); i++) {
            ServiceCenter center = centers.get(i);
            managers.add(new Manager(UUID.randomUUID(), "Manager " + (i + 1), "manager" + (i + 1) + "@fixzone.lk", "+94772000" + i,
                    passwordEncoder.encode("pass123"), "MANAGER", true, LocalDateTime.now(), LocalDateTime.now(),
                    "system", LocalDateTime.now(), "system", "https://images.unsplash.cc/photo-" + (new String[]{"1472099645785-5658abf4ff4e", "1519085360753-af0119f7cbe7", "1507003211169-0a1dd7228f2d", "1500648767791-00dcc994a43e", "1494790108377-be9c29b29330"}[i % 5]) + "?q=80&w=150&h=150&auto=format&fit=crop", "MGR-00" + (i + 1),
                    center.getCenterId()));
        }
        managerRepository.saveAll(managers);

        // 6. Service Packages
        List<ServicePackage> packages = new ArrayList<>();
        String[] pkgNames = { "Full Service", "Brake Check", "Hybrid Diagnostics", "Interior Detailing", "Nano Coating" };
        BigDecimal[] prices = { new BigDecimal("18000.00"), new BigDecimal("4500.00"), new BigDecimal("12000.00"),
                new BigDecimal("7500.00"), new BigDecimal("45000.00") };
        for (ServiceCenter center : centers) {
            for (int i = 0; i < pkgNames.length; i++) {
                packages.add(new ServicePackage(UUID.randomUUID(), center, pkgNames[i] + " (" + center.getAddress() + ")",
                                "Package", "Premium " + pkgNames[i] + " in " + center.getAddress(), prices[i],
                                60 + (i * 30), true, LocalDateTime.now(), "system", LocalDateTime.now(), "system"));
            }
        }
        servicePackageRepository.saveAll(packages);

        // 7. & 8. Bookings & Invoices (Historical Data for Analytics)
        System.out.println("Seeding historical data for FIX001...");
        List<Booking> bookings = new ArrayList<>();
        List<Invoice> invoices = new ArrayList<>();
        List<PaymentRecord> paymentRecords = new ArrayList<>();
        List<BookingHistory> historicalHistories = new ArrayList<>();

        Owner mainOwner = owners.get(0);
        ServiceCenter mainCenter = centers.get(0);

        // Historical Data: Jan to Apr
        int[] bookingCounts = { 15, 22, 28, 35 };
        int[] months = { 1, 2, 3, 4 };

        for (int m = 0; m < months.length; m++) {
            for (int k = 0; k < bookingCounts[m]; k++) {
                LocalDateTime date = LocalDateTime.of(2026, months[m], (k % 28) + 1, 10, 0);

                Booking b = new Booking();
                b.setBookingId(UUID.randomUUID());
                b.setCenterId(mainCenter.getCenterId());
                b.setTenantId(mainOwner.getUserId());
                b.setCustomerId(customers.get(k % 5).getUserId());
                b.setVehicleId(UUID.randomUUID());
                b.setPackageId(packages.get(k % 5).getPackageId());
                b.setBookingDate(date.toLocalDate().plusDays(1));
                b.setBookingTime(date.toLocalTime());
                b.setStatus(k % 10 == 0 ? BookingStatus.PENDING_PAYMENT : BookingStatus.COMPLETED);
                b.setEstimatedCost(packages.get(k % 5).getBasePrice());
                b.setCreatedAt(date);
                bookings.add(b);

                if (b.getStatus() == BookingStatus.COMPLETED) {
                    UUID invId = UUID.randomUUID();
                    BigDecimal total = b.getEstimatedCost();
                    BigDecimal tax = total.multiply(new BigDecimal("0.1")).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal subtot = total.subtract(tax);

                    Invoice inv = new Invoice(invId, mainOwner.getOwnerCode(), mainCenter.getCenterId(),
                            b.getBookingId(), b.getCustomerId(), subtot, tax, BigDecimal.ZERO, total, "PAID",
                            date.plusHours(2), date.plusDays(1), date, "system", date, "system");
                    invoices.add(inv);

                    paymentRecords.add(new PaymentRecord(UUID.randomUUID(), invId, mainCenter.getCenterId(), total, "CASH",
                            "TXN-" + months[m] + "-" + k, "Completed", date.plusHours(2), date.plusHours(2), "system",
                            date.plusHours(2), "system"));
                    
                    // Add history
                    BookingHistory hist = new BookingHistory();
                    hist.setBookingId(b.getBookingId());
                    hist.setTenantId(mainOwner.getUserId());
                    hist.setAction(BookingAction.COMPLETED);
                    hist.setNewDate(b.getBookingDate());
                    hist.setNewTime(b.getBookingTime());
                    hist.setPenalty(BigDecimal.ZERO);
                    hist.setNote("Service completed and paid via CASH.");
                    historicalHistories.add(hist);
                }
            }
        }

        // Additional status diversity for current month
        for (int i = 0; i < 10; i++) {
            ServiceCenter center = centers.get(i % centers.size());
            Customer customer = customers.get(i % customers.size());
            Booking b = new Booking();
            b.setBookingId(UUID.randomUUID());
            b.setCenterId(center.getCenterId());
            b.setTenantId(((Owner)center.getOwner()).getUserId());
            b.setCustomerId(customer.getUserId());
            b.setVehicleId(UUID.randomUUID());
            b.setPackageId(packages.get(i % packages.size()).getPackageId());
            b.setBookingDate(LocalDate.now().plusDays(i + 1));
            b.setBookingTime(LocalTime.of(9 + (i % 5), 0));
            b.setEstimatedCost(packages.get(i % packages.size()).getBasePrice());
            b.setCreatedAt(LocalDateTime.now().minusDays(1));

            switch (i % 4) {
                case 0:
                    b.setStatus(BookingStatus.PENDING_PAYMENT);
                    break;
                case 1:
                    b.setStatus(BookingStatus.CONFIRMED);
                    b.setBookingFeePaid(true);
                    break;
                case 2:
                    b.setStatus(BookingStatus.CANCELLED);
                    b.setIsCancelled(true);
                    b.setCancelledAt(LocalDateTime.now());
                    break;
                case 3:
                    b.setStatus(BookingStatus.COMPLETED);
                    b.setBookingFeePaid(true);
                    break;
            }
            bookings.add(b);
        }

        bookingRepository.saveAll(bookings);
        invoiceRepository.saveAll(invoices);
        paymentRecordRepository.saveAll(paymentRecords);
        bookingHistoryRepository.saveAll(historicalHistories);

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

        // --- PENDING REGISTRATION REQUESTS ---
        System.out.println("Adding PENDING registration requests...");
        Owner pendingOwner = new Owner(UUID.randomUUID(), "Kusal Mendis", "kusal@test.com", "+94775000000",
                passwordEncoder.encode("pass123"), "OWNER", true, LocalDateTime.now(), LocalDateTime.now(), "system",
                LocalDateTime.now(), "system", "https://i.pravatar.cc/150?u=Kusal+Mendis", "FIXPEND01", "Mendis Auto",
                "contact@mendis.lk", "+94112555555");
        ownerRepository.save(pendingOwner);

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

        // 12. Payments
        System.out.println("Seeding general payments...");
        List<Payment> paymentList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Payment p = new Payment();
            p.setBookingId((long) (i + 1));
            p.setAmount(5000.0 + (i * 1000));
            p.setStatus(PaymentStatus.PAID);
            p.setDate("2026-04-" + (10 + i));
            p.setTimeSlot("10:00 AM");
            paymentList.add(p);
        }
        paymentRepository.saveAll(paymentList);

        System.out.println("--- SRI LANKAN DATA SEEDING COMPLETE ---");
    }
}
