package com.fixzone.fixzon_backend;

import com.fixzone.fixzon_backend.model.Customer;
import com.fixzone.fixzon_backend.model.Manager;
import com.fixzone.fixzon_backend.model.Owner;
import com.fixzone.fixzon_backend.model.SuperAdmin;
import com.fixzone.fixzon_backend.model.User;
import com.fixzone.fixzon_backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

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
	public CommandLineRunner dataLoader(UserRepository userRepository) {
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

				// Customers (Names matching frontend dummy data)
				users.add(new Customer(UUID.fromString("00000000-0000-0000-0000-000000010006"), "Kasun Perera",
						"kasun.perera@example.com", "+94771234567", "pass123", "CUSTOMER", true, LocalDateTime.now(),
						LocalDateTime.now(), "system", LocalDateTime.now(), "system", "CUST-001", "EMAIL",
						12, 15600.0, "VIP", "https://i.pravatar.cc/150?u=1"));
				users.add(new Customer(UUID.fromString("00000000-0000-0000-0000-000000010007"), "Nimali Silva",
						"nimali.silva@example.com", "+94711234568", "pass123", "CUSTOMER", true, LocalDateTime.now(),
						LocalDateTime.now(), "system", LocalDateTime.now(), "system", "CUST-002", "PHONE",
						5, 3500.0, "Active", "https://i.pravatar.cc/150?u=2"));
				users.add(new Customer(UUID.fromString("00000000-0000-0000-0000-000000010008"), "Ruwan Fernando",
						"ruwan.f@example.com", "+94761234569", "pass123", "CUSTOMER", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system",
						"CUST-003", "SMS", 8, 8900.0, "Active", "https://i.pravatar.cc/150?u=3"));
				users.add(new Customer(UUID.fromString("00000000-0000-0000-0000-000000010009"), "Dilshan Bandara",
						"dilshan.b@example.com", "+94701234570", "pass123", "CUSTOMER", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system",
						"CUST-004", "EMAIL", 1, 450.0, "New", "https://i.pravatar.cc/150?u=4"));
				users.add(new Customer(UUID.fromString("00000000-0000-0000-0000-000000010010"), "Chamari Atapattu",
						"chamari.a@example.com", "+94751234571", "pass123", "CUSTOMER", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system",
						"CUST-005", "PHONE", 20, 25000.0, "VIP", "https://i.pravatar.cc/150?u=5"));

				// Owners
				users.add(new Owner(UUID.fromString("00000000-0000-0000-0000-000000010011"), "Elizabeth Taylor",
						"e.tay Taylor@fixzone.com", "+12025550111", "pass123", "OWNER", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "OWN-001",
						"Taylor Logistics", "contact@taylorlogs.com", "+15550111"));
				users.add(new Owner(UUID.fromString("00000000-0000-0000-0000-000000010012"), "Richard Moore",
						"richard.moore@fixzone.com", "+12025550112", "pass123", "OWNER", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "OWN-002",
						"Moore Repairs", "info@moore.com", "+15550112"));
				users.add(new Owner(UUID.fromString("00000000-0000-0000-0000-000000010013"), "Susan Anderson",
						"s.anderson@fixzone.com", "+12025550113", "pass123", "OWNER", true, LocalDateTime.now(),
						LocalDateTime.now(), "system", LocalDateTime.now(), "system", "OWN-003", "Anderson Autos",
						"sales@anderson.com", "+15550113"));
				users.add(new Owner(UUID.fromString("00000000-0000-0000-0000-000000010014"), "Thomas Jackson",
						"t.jackson@fixzone.com", "+12025550114", "pass123", "OWNER", true, LocalDateTime.now(),
						LocalDateTime.now(), "system", LocalDateTime.now(), "system", "OWN-004", "Jackson Tech",
						"support@jackson.com", "+15550114"));
				users.add(new Owner(UUID.fromString("00000000-0000-0000-0000-000000010015"), "Margaret Harris",
						"m.harris@fixzone.com", "+12025550115", "pass123", "OWNER", true, LocalDateTime.now(),
						LocalDateTime.now(), "system", LocalDateTime.now(), "system", "OWN-005", "Harris Group",
						"margaret@harris.com", "+15550115"));

				// Managers
				UUID center1Id = UUID.fromString("c0000000-0000-0000-0000-000000000001");
				UUID center2Id = UUID.fromString("c0000000-0000-0000-0000-000000000002");

				users.add(new Manager(UUID.fromString("00000000-0000-0000-0000-000000010016"), "Steven Clark",
						"s.clark@fixzone.com", "+12025550116", "pass123", "MANAGER", true, LocalDateTime.now(),
						LocalDateTime.now(), "system", LocalDateTime.now(), "system", "MGR-001", center1Id));
				users.add(new Manager(UUID.fromString("00000000-0000-0000-0000-000000010017"), "Mary Lewis",
						"mary.lewis@fixzone.com", "+12025550117", "pass123", "MANAGER", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "MGR-002",
						center1Id));
				users.add(new Manager(UUID.fromString("00000000-0000-0000-0000-000000010018"), "Paul Walker",
						"paul.walker@fixzone.com", "+12025550118", "pass123", "MANAGER", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "MGR-003",
						center2Id));
				users.add(new Manager(UUID.fromString("00000000-0000-0000-0000-000000010019"), "Karen Young",
						"k.young@fixzone.com", "+12025550119", "pass123", "MANAGER", true, LocalDateTime.now(),
						LocalDateTime.now(), "system", LocalDateTime.now(), "system", "MGR-004", center2Id));
				users.add(new Manager(UUID.fromString("00000000-0000-0000-0000-000000010020"), "Mark Allen",
						"mark.allen@fixzone.com", "+12025550120", "pass123", "MANAGER", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "MGR-005",
						center1Id));

				userRepository.saveAll(users);
				System.out.println("Realistic mock users loaded into database.");
			}
		};
	}
}
