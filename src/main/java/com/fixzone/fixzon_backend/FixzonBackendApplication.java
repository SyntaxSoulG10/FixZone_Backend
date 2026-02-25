package com.fixzone.fixzon_backend;

import com.fixzone.fixzon_backend.model.Customer;
import com.fixzone.fixzon_backend.model.User;
import com.fixzone.fixzon_backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;
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
				List<User> users = List.of(
						// Admins
						new User(UUID.fromString("00000000-0000-0000-0000-000000010001"), "James Wilson",
								"james.wilson@fixzone.com", "+12025550101", "pass123", "ADMIN", true,
								LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system"),
						new User(UUID.fromString("00000000-0000-0000-0000-000000010002"), "Linda Garcia",
								"linda.garcia@fixzone.com", "+12025550102", "pass123", "ADMIN", true,
								LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system"),
						new User(UUID.fromString("00000000-0000-0000-0000-000000010003"), "Robert Chen",
								"robert.chen@fixzone.com", "+12025550103", "pass123", "ADMIN", true,
								LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system"),
						new User(UUID.fromString("00000000-0000-0000-0000-000000010004"), "Sarah Miller",
								"sarah.miller@fixzone.com", "+12025550104", "pass123", "ADMIN", true,
								LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system"),
						new User(UUID.fromString("00000000-0000-0000-0000-000000010005"), "Michael Davis",
								"michael.davis@fixzone.com", "+12025550105", "pass123", "ADMIN", true,
								LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system"),

						// Customers
						new Customer(UUID.fromString("00000000-0000-0000-0000-000000010006"), "David Thompson",
								"david.t@example.com", "+12025550106", "pass123", "CUSTOMER", true, LocalDateTime.now(),
								LocalDateTime.now(), "system", LocalDateTime.now(), "system", "CUST-001", "EMAIL"),
						new Customer(UUID.fromString("00000000-0000-0000-0000-000000010007"), "Jennifer Lopez",
								"j.lopez@example.com", "+12025550107", "pass123", "CUSTOMER", true, LocalDateTime.now(),
								LocalDateTime.now(), "system", LocalDateTime.now(), "system", "CUST-002", "PHONE"),
						new Customer(UUID.fromString("00000000-0000-0000-0000-000000010008"), "Chris Lee",
								"chris.lee@example.com", "+12025550108", "pass123", "CUSTOMER", true,
								LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system",
								"CUST-003", "SMS"),
						new Customer(UUID.fromString("00000000-0000-0000-0000-000000010009"), "Amanda White",
								"amanda.white@example.com", "+12025550109", "pass123", "CUSTOMER", true,
								LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system",
								"CUST-004", "EMAIL"),
						new Customer(UUID.fromString("00000000-0000-0000-0000-000000010010"), "Kevin Martin",
								"kevin.martin@example.com", "+12025550110", "pass123", "CUSTOMER", true,
								LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system",
								"CUST-005", "PHONE"),

						// Owners
						new User(UUID.fromString("00000000-0000-0000-0000-000000010011"), "Elizabeth Taylor",
								"e.tay Taylor@fixzone.com", "+12025550111", "pass123", "OWNER", true,
								LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system"),
						new User(UUID.fromString("00000000-0000-0000-0000-000000010012"), "Richard Moore",
								"richard.moore@fixzone.com", "+12025550112", "pass123", "OWNER", true,
								LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system"),
						new User(UUID.fromString("00000000-0000-0000-0000-000000010013"), "Susan Anderson",
								"s.anderson@fixzone.com", "+12025550113", "pass123", "OWNER", true, LocalDateTime.now(),
								LocalDateTime.now(), "system", LocalDateTime.now(), "system"),
						new User(UUID.fromString("00000000-0000-0000-0000-000000010014"), "Thomas Jackson",
								"t.jackson@fixzone.com", "+12025550114", "pass123", "OWNER", true, LocalDateTime.now(),
								LocalDateTime.now(), "system", LocalDateTime.now(), "system"),
						new User(UUID.fromString("00000000-0000-0000-0000-000000010015"), "Margaret Harris",
								"m.harris@fixzone.com", "+12025550115", "pass123", "OWNER", true, LocalDateTime.now(),
								LocalDateTime.now(), "system", LocalDateTime.now(), "system"),

						// Managers
						new User(UUID.fromString("00000000-0000-0000-0000-000000010016"), "Steven Clark",
								"s.clark@fixzone.com", "+12025550116", "pass123", "MANAGER", true, LocalDateTime.now(),
								LocalDateTime.now(), "system", LocalDateTime.now(), "system"),
						new User(UUID.fromString("00000000-0000-0000-0000-000000010017"), "Mary Lewis",
								"mary.lewis@fixzone.com", "+12025550117", "pass123", "MANAGER", true,
								LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system"),
						new User(UUID.fromString("00000000-0000-0000-0000-000000010018"), "Paul Walker",
								"paul.walker@fixzone.com", "+12025550118", "pass123", "MANAGER", true,
								LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system"),
						new User(UUID.fromString("00000000-0000-0000-0000-000000010019"), "Karen Young",
								"k.young@fixzone.com", "+12025550119", "pass123", "MANAGER", true, LocalDateTime.now(),
								LocalDateTime.now(), "system", LocalDateTime.now(), "system"),
						new User(UUID.fromString("00000000-0000-0000-0000-000000010020"), "Mark Allen",
								"mark.allen@fixzone.com", "+12025550120", "pass123", "MANAGER", true,
								LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system"));
				userRepository.saveAll(users);
				System.out.println("20 realistic mock users with 5-digit sequence IDs loaded into database.");
			}
		};
	}
}
