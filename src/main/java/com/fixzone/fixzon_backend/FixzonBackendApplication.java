package com.fixzone.fixzon_backend;

import com.fixzone.fixzon_backend.model.Customer;
import com.fixzone.fixzon_backend.model.Manager;
import com.fixzone.fixzon_backend.model.Owner;
import com.fixzone.fixzon_backend.model.SuperAdmin;
import com.fixzone.fixzon_backend.model.User;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
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
			ServiceCenterRepository serviceCenterRepository) {
		return args -> {
			if (userRepository.count() == 0) {
				List<User> users = new ArrayList<>();

				// Admins (Initialized as SuperAdmin to appear in super_admin table)
				users.add(new SuperAdmin(UUID.fromString("00000000-0000-0000-0000-000000010001"), "James Wilson",
						"james.wilson@fixzone.com", "+12025550101", "pass123", "ADMIN", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "AD-001"));
				users.add(new SuperAdmin(UUID.fromString("00000000-0000-0000-0000-000000010002"), "Linda Garcia",
						"linda.garcia@fixzone.com", "+12025550102", "pass123", "ADMIN", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "AD-002"));
				users.add(new SuperAdmin(UUID.fromString("00000000-0000-0000-0000-000000010003"), "Robert Chen",
						"robert.chen@fixzone.com", "+12025550103", "pass123", "ADMIN", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "AD-003"));
				users.add(new SuperAdmin(UUID.fromString("00000000-0000-0000-0000-000000010004"), "Sarah Miller",
						"sarah.miller@fixzone.com", "+12025550104", "pass123", "ADMIN", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "AD-004"));
				users.add(new SuperAdmin(UUID.fromString("00000000-0000-0000-0000-000000010005"), "Michael Davis",
						"michael.davis@fixzone.com", "+12025550105", "pass123", "ADMIN", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "AD-005"));

				// Customers
				users.add(new Customer(UUID.fromString("00000000-0000-0000-0000-000000010006"), "David Thompson",
						"david.t@example.com", "+12025550106", "pass123", "CUSTOMER", true, LocalDateTime.now(),
						LocalDateTime.now(), "system", LocalDateTime.now(), "system", "CUST-001", "EMAIL"));
				users.add(new Customer(UUID.fromString("00000000-0000-0000-0000-000000010007"), "Jennifer Lopez",
						"j.lopez@example.com", "+12025550107", "pass123", "CUSTOMER", true, LocalDateTime.now(),
						LocalDateTime.now(), "system", LocalDateTime.now(), "system", "CUST-002", "PHONE"));
				users.add(new Customer(UUID.fromString("00000000-0000-0000-0000-000000010008"), "Chris Lee",
						"chris.lee@example.com", "+12025550108", "pass123", "CUSTOMER", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system",
						"CUST-003", "SMS"));
				users.add(new Customer(UUID.fromString("00000000-0000-0000-0000-000000010009"), "Amanda White",
						"amanda.white@example.com", "+12025550109", "pass123", "CUSTOMER", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system",
						"CUST-004", "EMAIL"));
				users.add(new Customer(UUID.fromString("00000000-0000-0000-0000-000000010010"), "Kevin Martin",
						"kevin.martin@example.com", "+12025550110", "pass123", "CUSTOMER", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system",
						"CUST-005", "PHONE"));

				// Owners
				Owner owner1 = new Owner(UUID.fromString("00000000-0000-0000-0000-000000010011"), "Elizabeth Taylor",
						"e.taylor@fixzone.com", "+12025550111", "pass123", "OWNER", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "OWN-001",
						"Taylor Logistics", "contact@taylorlogs.com", "+15550111");
				users.add(owner1);

				Owner owner2 = new Owner(UUID.fromString("00000000-0000-0000-0000-000000010012"), "Richard Moore",
						"richard.moore@fixzone.com", "+12025550112", "pass123", "OWNER", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "OWN-002",
						"Moore Repairs", "info@moore.com", "+15550112");
				users.add(owner2);

				Owner owner3 = new Owner(UUID.fromString("00000000-0000-0000-0000-000000010013"), "Susan Anderson",
						"s.anderson@fixzone.com", "+12025550113", "pass123", "OWNER", true, LocalDateTime.now(),
						LocalDateTime.now(), "system", LocalDateTime.now(), "system", "OWN-003", "Anderson Autos",
						"sales@anderson.com", "+15550113");
				users.add(owner3);

				Owner owner4 = new Owner(UUID.fromString("00000000-0000-0000-0000-000000010014"), "Thomas Jackson",
						"t.jackson@fixzone.com", "+12025550114", "pass123", "OWNER", true, LocalDateTime.now(),
						LocalDateTime.now(), "system", LocalDateTime.now(), "system", "OWN-004", "Jackson Tech",
						"support@jackson.com", "+15550114");
				users.add(owner4);

				Owner owner5 = new Owner(UUID.fromString("00000000-0000-0000-0000-000000010015"), "Margaret Harris",
						"m.harris@fixzone.com", "+12025550115", "pass123", "OWNER", true, LocalDateTime.now(),
						LocalDateTime.now(), "system", LocalDateTime.now(), "system", "OWN-005", "Harris Group",
						"margaret@harris.com", "+15550115");
				users.add(owner5);

				// Managers are not seeded here to avoid foreign-key issues with service centers
				UUID center1Id = UUID.fromString("c0000000-0000-0000-0000-000000000001");
				UUID center2Id = UUID.fromString("c0000000-0000-0000-0000-000000000002");

				userRepository.saveAll(users);
				System.out.println("Realistic mock users loaded into database.");

				// Service Centers
				if (serviceCenterRepository.count() == 0) {
					List<ServiceCenter> centers = new ArrayList<>();

					centers.add(new ServiceCenter(
							center1Id,
							owner1,
							"Taylor Express Maintenance",
							"123 Main St, New York",
							"+1-555-0101",
							"08:00 - 18:00",
							new BigDecimal("4.8"),
							true,
							LocalDateTime.now(), "system", LocalDateTime.now(), "system",
							new String[] { "Toyota", "Honda", "Ford" }));

					centers.add(new ServiceCenter(
							center2Id,
							owner2,
							"Moore Precision Repairs",
							"456 Oak Ave, Los Angeles",
							"+1-555-0102",
							"09:00 - 19:00",
							new BigDecimal("4.5"),
							true,
							LocalDateTime.now(), "system", LocalDateTime.now(), "system",
							new String[] { "BMW", "Mercedes", "Audi" }));

					serviceCenterRepository.saveAll(centers);
					System.out.println("Mock service centers loaded into database.");
				}
			}
		};
	}
}
