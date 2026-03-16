package com.fixzone.fixzon_backend;

import com.fixzone.fixzon_backend.model.Customer;
import com.fixzone.fixzon_backend.model.Owner;
import com.fixzone.fixzon_backend.model.User;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.model.Invoice;
import com.fixzone.fixzon_backend.model.PaymentRecord;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.UserRepository;
import com.fixzone.fixzon_backend.repository.InvoiceRepository;
import com.fixzone.fixzon_backend.repository.PaymentRecordRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SpringBootApplication
public class FixzonBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(FixzonBackendApplication.class, args);
    }

    @Bean
    public CommandLineRunner dataLoader(UserRepository userRepository,
                                        ServiceCenterRepository serviceCenterRepository,
                                        InvoiceRepository invoiceRepository,
                                        PaymentRecordRepository paymentRecordRepository) {
        return args -> {
            System.out.println("--- STARTING DATA SEEDING ---");
            
            // 1. Create Baseline Data (Owners, Customers, Centers)
            if (userRepository.count() == 0) {
                List<User> users = new ArrayList<>();
                Owner owner1 = new Owner(UUID.fromString("00000000-0000-0000-0000-000000010011"), "Elizabeth Taylor", "e.taylor@fixzone.com", "+12025550111", "pass123", "OWNER", true, LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "OWN-001", "Taylor Logistics", "contact@taylorlogs.com", "+15550111");
                Owner owner2 = new Owner(UUID.fromString("00000000-0000-0000-0000-000000010012"), "Richard Moore", "richard.moore@fixzone.com", "+12025550112", "pass123", "OWNER", true, LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "OWN-002", "Moore Repairs", "info@moore.com", "+15550112");
                users.add(owner1);
                users.add(owner2);
                users.add(new Customer(UUID.fromString("00000000-0000-0000-0000-000000010006"), "David Thompson", "david.t@example.com", "+12025550106", "pass123", "CUSTOMER", true, LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "CUST-001", "EMAIL"));
                userRepository.saveAll(users);
                
                List<ServiceCenter> centers = new ArrayList<>();
                centers.add(new ServiceCenter(UUID.fromString("c0000000-0000-0000-0000-000000000001"), owner1, "Taylor Express NY", "New York", "+1-555-0101", "08:00 - 18:00", new BigDecimal("4.8"), true, LocalDateTime.now(), "system", LocalDateTime.now(), "system", new String[]{"Toyota", "Honda"}));
                centers.add(new ServiceCenter(UUID.fromString("c0000000-0000-0000-0000-000000000002"), owner2, "Moore Precision LA", "Los Angeles", "+1-555-0102", "09:00 - 19:00", new BigDecimal("4.5"), true, LocalDateTime.now(), "system", LocalDateTime.now(), "system", new String[]{"BMW", "Audi"}));
                serviceCenterRepository.saveAll(centers);
                System.out.println("Base users and centers seeded.");
            }

            if (invoiceRepository.count() < 150) { 
                System.out.println("Generating 150 truly random financial records...");
                paymentRecordRepository.deleteAll();
                invoiceRepository.deleteAll();

                UUID center1Id = UUID.fromString("c0000000-0000-0000-0000-000000000001");
                UUID center2Id = UUID.fromString("c0000000-0000-0000-0000-000000000002");
                UUID customerId = UUID.fromString("00000000-0000-0000-0000-000000010006");
                LocalDateTime now = LocalDateTime.now();

                List<Invoice> invs = new ArrayList<>();
                List<PaymentRecord> pays = new ArrayList<>();

                for (int i = 0; i < 150; i++) {
                    UUID invId = UUID.randomUUID();
                    long daysBack = (long)(Math.random() * 180);
                    LocalDateTime dt = now.minusDays(daysBack);
                    
                    // Truly random values, no fixed formula
                    double randOnline = 150.0 + (Math.random() * 450.0); // 150 to 600
                    double randCash = 700.0 + (Math.random() * 2300.0); // 700 to 3000

                    BigDecimal onlineAmt = new BigDecimal(randOnline).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal cashAmt = new BigDecimal(randCash).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal totalAmt = onlineAmt.add(cashAmt);

                    BigDecimal tax = totalAmt.multiply(new BigDecimal("0.1")).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal subtotal = totalAmt.subtract(tax);

                    invs.add(new Invoice(invId, "INV-RND-" + (1000 + i), (Math.random() > 0.5) ? center1Id : center2Id, null, customerId, 
                        subtotal, tax, BigDecimal.ZERO, totalAmt, "PAID", dt, dt, dt, "system", dt, "system"));

                    pays.add(new PaymentRecord(UUID.randomUUID(), invId, (Math.random() > 0.5) ? center1Id : center2Id, 
                        onlineAmt, (Math.random() > 0.5) ? "CREDIT_CARD" : "ONLINE_BANKING", "TXN-RD-" + i, "Completed", dt, dt, "system", dt, "system"));
                }
                invoiceRepository.saveAll(invs);
                paymentRecordRepository.saveAll(pays);
                System.out.println("150 random financial records seeded.");
            }
            System.out.println("--- DATA SEEDING COMPLETE ---");
        };
    }
}
