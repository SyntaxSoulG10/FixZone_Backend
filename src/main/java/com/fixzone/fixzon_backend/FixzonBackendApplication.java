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

				// Admins
				users.add(new SuperAdmin(UUID.fromString("00000000-0000-0000-0000-000000010001"), "James Wilson",
						"james.wilson@fixzone.com", "+12025550101", "pass123", "ADMIN", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "AD-001"));
				users.add(new SuperAdmin(UUID.fromString("00000000-0000-0000-0000-000000010002"), "Linda Garcia",
						"linda.garcia@fixzone.com", "+12025550102", "pass123", "ADMIN", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "AD-002"));

				// Customers
				users.add(new Customer(UUID.fromString("00000000-0000-0000-0000-000000010006"), "David Thompson",
						"david.t@example.com", "+12025550106", "pass123", "CUSTOMER", true, LocalDateTime.now(),
						LocalDateTime.now(), "system", LocalDateTime.now(), "system", "CUST-001", "EMAIL"));
				users.add(new Customer(UUID.fromString("00000000-0000-0000-0000-000000020001"), "Tharanga Paranavitana",
						"t.paranavitana@example.com", "+94770000001", "pass123", "CUSTOMER", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system",
						"CUST-006", "EMAIL"));

				// Owners
				Owner owner1 = new Owner(UUID.fromString("00000000-0000-0000-0000-000000010011"), "Sunil Perera",
						"sunil.perera@fixzone.lk", "+94771234567", "pass123", "OWNER", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "OWN-001",
						"Sunil Auto Care", "contact@sunilauto.lk", "+94112345678");
				users.add(owner1);

				Owner owner2 = new Owner(UUID.fromString("00000000-0000-0000-0000-000000010012"), "Kasun Jayawardena",
						"kasun.j@fixzone.lk", "+94772345678", "pass123", "OWNER", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "OWN-002",
						"KJ Motors", "info@kjmotors.lk", "+94112345679");
				users.add(owner2);

				Owner owner3 = new Owner(UUID.fromString("00000000-0000-0000-0000-000000010013"), "Nilanthi Silva",
						"n.silva@fixzone.lk", "+94773456789", "pass123", "OWNER", true, LocalDateTime.now(),
						LocalDateTime.now(), "system", LocalDateTime.now(), "system", "OWN-003",
						"Silva Service Station", "sales@silvaservice.lk", "+94112345680");
				users.add(owner3);

				Owner owner4 = new Owner(UUID.fromString("00000000-0000-0000-0000-000000010014"), "Arjun Ratnayake",
						"arjun.r@fixzone.lk", "+94774567890", "pass123", "OWNER", true, LocalDateTime.now(),
						LocalDateTime.now(), "system", LocalDateTime.now(), "system", "OWN-004", "Ratnayake Tech",
						"support@ratnayake.lk", "+94112345681");
				users.add(owner4);

				Owner owner5 = new Owner(UUID.fromString("00000000-0000-0000-0000-000000010015"), "Ishara Fernando",
						"ishara.f@fixzone.lk", "+94775678901", "pass123", "OWNER", true, LocalDateTime.now(),
						LocalDateTime.now(), "system", LocalDateTime.now(), "system", "OWN-005", "Fernando Group",
						"ishara@fernando.lk", "+94112345682");
				users.add(owner5);

				// Managers
				UUID center1Id = UUID.fromString("c0000000-0000-0000-0000-000000000001");
				UUID center2Id = UUID.fromString("c0000000-0000-0000-0000-000000000002");
				UUID center3Id = UUID.fromString("c0000000-0000-0000-0000-000000000003");
				UUID center4Id = UUID.fromString("c0000000-0000-0000-0000-000000000004");
				UUID center5Id = UUID.fromString("c0000000-0000-0000-0000-000000000005");

				users.add(new Manager(UUID.fromString("00000000-0000-0000-0000-000000010016"), "John Doe",
						"john.d@fixzone.lk", "+94776789012", "pass123", "MANAGER", true, LocalDateTime.now(),
						LocalDateTime.now(), "system", LocalDateTime.now(), "system", "MGR-001", center1Id));
				users.add(new Manager(UUID.fromString("00000000-0000-0000-0000-000000010017"), "Jane Smith",
						"jane.s@fixzone.lk", "+94777890123", "pass123", "MANAGER", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "MGR-002",
						center2Id));

				userRepository.saveAll(users);
				System.out.println("Mock users loaded.");
			}

			if (serviceCenterRepository.count() == 0) {
				List<ServiceCenter> centers = new ArrayList<>();

				// Re-fetch owners from DB or use the ones we just added if we're in the same
				// transaction
				// For CommandLineRunner, they are committed after the lambda finishes unless we
				// use a transaction.
				// But we can just use the objects we have.

				Owner owner1 = (Owner) userRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000010011"))
						.orElse(null);
				Owner owner2 = (Owner) userRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000010012"))
						.orElse(null);
				Owner owner3 = (Owner) userRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000010013"))
						.orElse(null);
				Owner owner4 = (Owner) userRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000010014"))
						.orElse(null);
				Owner owner5 = (Owner) userRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000010015"))
						.orElse(null);

				centers.add(new ServiceCenter(
						UUID.fromString("c0000000-0000-0000-0000-000000000001"),
						owner1,
						"Downtown Branch",
						"20, Green Road, Gampaha",
						"0708891234",
						"08:00 - 18:00",
						new BigDecimal("4.8"),
						true,
						LocalDateTime.now(), "system", LocalDateTime.now(), "system",
						new String[] { "Toyota", "Honda", "Suzuki" },
						"John Doe",
						new BigDecimal("45200"),
						12,
						85));

				centers.add(new ServiceCenter(
						UUID.fromString("c0000000-0000-0000-0000-000000000002"),
						owner2,
						"Westside Hub",
						"38, Green Road, Colombo",
						"0777614531",
						"09:00 - 19:00",
						new BigDecimal("4.5"),
						true,
						LocalDateTime.now(), "system", LocalDateTime.now(), "system",
						new String[] { "Toyota", "Nissan", "Mitsubishi" },
						"Jane Smith",
						new BigDecimal("32100"),
						8,
						45));

				centers.add(new ServiceCenter(
						UUID.fromString("c0000000-0000-0000-0000-000000000003"),
						owner3,
						"Silva Service Station - Mattegoda",
						"789 Kottawa Road, Mattegoda",
						"+94-112-341111",
						"07:30 - 19:30",
						new BigDecimal("4.9"),
						true,
						LocalDateTime.now(), "system", LocalDateTime.now(), "system",
						new String[] { "Honda", "Suzuki", "Bajaj" },
						"Isuru Udana",
						new BigDecimal("67500"),
						5,
						30));

				centers.add(new ServiceCenter(
						UUID.fromString("c0000000-0000-0000-0000-000000000004"),
						owner4,
						"Ratnayake Tech & Auto - Gampaha",
						"101 Gampaha Road, Gampaha",
						"+94-332-225566",
						"08:30 - 17:30",
						new BigDecimal("4.7"),
						true,
						LocalDateTime.now(), "system", LocalDateTime.now(), "system",
						new String[] { "Toyota", "Nissan", "Mazda" },
						"Kusal Mendis",
						new BigDecimal("112000"),
						10,
						60));

				centers.add(new ServiceCenter(
						UUID.fromString("c0000000-0000-0000-0000-000000000005"),
						owner5,
						"Fernando Elite Maintenance - Negombo",
						"55 Beach Road, Negombo",
						"+94-312-223344",
						"08:00 - 20:00",
						new BigDecimal("4.6"),
						true,
						LocalDateTime.now(), "system", LocalDateTime.now(), "system",
						new String[] { "Mitsubishi", "Kia", "Hyundai" },
						"Ishara Fernando",
						new BigDecimal("88000"),
						7,
						50));

				serviceCenterRepository.saveAll(centers);
				System.out.println("Mock service centers loaded.");
			}
		};
	}
}
