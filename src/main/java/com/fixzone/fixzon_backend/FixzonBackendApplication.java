package com.fixzone.fixzon_backend;

import com.fixzone.fixzon_backend.model.Customer;
import com.fixzone.fixzon_backend.model.Manager;
import com.fixzone.fixzon_backend.model.Owner;
import com.fixzone.fixzon_backend.model.SuperAdmin;
import com.fixzone.fixzon_backend.model.User;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.model.ServicePackage;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.ServicePackageRepository;
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
			ServiceCenterRepository serviceCenterRepository,
			ServicePackageRepository servicePackageRepository) {
		return args -> {
			if (userRepository.count() == 0) {
				List<User> users = new ArrayList<>();

				// Admins (Initialized as SuperAdmin to appear in super_admin table)
				users.add(new SuperAdmin(UUID.fromString("00000000-0000-0000-0000-000000010001"), "Kusal Perera",
						"kusal.perera@fixzone.lk", "+94771234567", "pass123", "ADMIN", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "AD-001"));
				users.add(new SuperAdmin(UUID.fromString("00000000-0000-0000-0000-000000010002"), "Dilini Jayawardena",
						"dilini.j@fixzone.lk", "+94772345678", "pass123", "ADMIN", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "AD-002"));
				users.add(new SuperAdmin(UUID.fromString("00000000-0000-0000-0000-000000010003"), "Manoj Gunaratne",
						"manoj.g@fixzone.lk", "+94773456789", "pass123", "ADMIN", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "AD-003"));
				users.add(new SuperAdmin(UUID.fromString("00000000-0000-0000-0000-000000010004"), "Nilupul Silva",
						"nilupul.s@fixzone.lk", "+94774567890", "pass123", "ADMIN", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "AD-004"));
				users.add(new SuperAdmin(UUID.fromString("00000000-0000-0000-0000-000000010005"), "Chaminda Silva",
						"chaminda.s@fixzone.lk", "+94775678901", "pass123", "ADMIN", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "AD-005"));

				// Customers
				users.add(new Customer(UUID.fromString("00000000-0000-0000-0000-000000010006"), "Aruna Kumara",
						"aruna.k@gmail.com", "+94711122334", "pass123", "CUSTOMER", true, LocalDateTime.now(),
						LocalDateTime.now(), "system", LocalDateTime.now(), "system", "CUST-001", "EMAIL"));
				users.add(new Customer(UUID.fromString("00000000-0000-0000-0000-000000010007"), "Malini Perera",
						"malini.p@yahoo.com", "+94712233445", "pass123", "CUSTOMER", true, LocalDateTime.now(),
						LocalDateTime.now(), "system", LocalDateTime.now(), "system", "CUST-002", "PHONE"));
				users.add(new Customer(UUID.fromString("00000000-0000-0000-0000-000000010008"), "Suresh Lakmal",
						"suresh.l@outlook.com", "+94713344556", "pass123", "CUSTOMER", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system",
						"CUST-003", "SMS"));
				users.add(new Customer(UUID.fromString("00000000-0000-0000-0000-000000010009"), "Ishani Maduwanthi",
						"ishani.m@gmail.com", "+94714455667", "pass123", "CUSTOMER", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system",
						"CUST-004", "EMAIL"));
				users.add(new Customer(UUID.fromString("00000000-0000-0000-0000-000000010010"), "Ruwan Bandara",
						"ruwan.b@gmail.com", "+94715566778", "pass123", "CUSTOMER", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system",
						"CUST-005", "PHONE"));

				// Owners
				Owner owner1 = new Owner(UUID.fromString("00000000-0000-0000-0000-000000010011"), "Kamal Gunaratne",
						"kamal.g@lankaauto.lk", "+94701234567", "pass123", "OWNER", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "OWN-001",
						"Lanka Auto Solutions", "contact@lankaauto.lk", "+94112345678");
				users.add(owner1);

				Owner owner2 = new Owner(UUID.fromString("00000000-0000-0000-0000-000000010012"), "Nuwan Sameera",
						"nuwan.s@hybridcare.lk", "+94702345678", "pass123", "OWNER", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "OWN-002",
						"Hybrid Care Lanka", "info@hybridcare.lk", "+94113456789");
				users.add(owner2);

				Owner owner3 = new Owner(UUID.fromString("00000000-0000-0000-0000-000000010013"),
						"Chathuri Priyadharshani",
						"chathuri.p@revv.lk", "+94703456789", "pass123", "OWNER", true, LocalDateTime.now(),
						LocalDateTime.now(), "system", LocalDateTime.now(), "system", "OWN-003", "Revv Motors",
						"sales@revv.lk", "+94114567890");
				users.add(owner3);

				Owner owner4 = new Owner(UUID.fromString("00000000-0000-0000-0000-000000010014"), "Upul Tharanga",
						"upul.t@southernfix.lk", "+94704567890", "pass123", "OWNER", true, LocalDateTime.now(),
						LocalDateTime.now(), "system", LocalDateTime.now(), "system", "OWN-004", "Southern Fix",
						"support@southernfix.lk", "+94912345678");
				users.add(owner4);

				Owner owner5 = new Owner(UUID.fromString("00000000-0000-0000-0000-000000010015"), "Sandunika De Silva",
						"sandunika.d@islandrepairs.lk", "+94705678901", "pass123", "OWNER", true, LocalDateTime.now(),
						LocalDateTime.now(), "system", LocalDateTime.now(), "system", "OWN-005", "Island Repairs",
						"admin@islandrepairs.lk", "+94115678901");
				users.add(owner5);

				// Managers
				UUID center1Id = UUID.fromString("c0000000-0000-0000-0000-000000000001");
				UUID center2Id = UUID.fromString("c0000000-0000-0000-0000-000000000002");

				// Managers
				users.add(new Manager(UUID.fromString("00000000-0000-0000-0000-000000010016"), "Pathum Nissanka",
						"pathum.n@fixzone.lk", "+94721234567", "pass123", "MANAGER", true, LocalDateTime.now(),
						LocalDateTime.now(), "system", LocalDateTime.now(), "system", "MGR-001", center1Id));
				users.add(new Manager(UUID.fromString("00000000-0000-0000-0000-000000010017"), "Vishakha Jayawardena",
						"vishakha.j@fixzone.lk", "+94722345678", "pass123", "MANAGER", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "MGR-002",
						center1Id));
				users.add(new Manager(UUID.fromString("00000000-0000-0000-0000-000000010018"), "Dinidu Buddhika",
						"dinidu.b@fixzone.lk", "+94723456789", "pass123", "MANAGER", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "MGR-003",
						center2Id));
				users.add(new Manager(UUID.fromString("00000000-0000-0000-0000-000000010019"), "Tharushi Karunaratne",
						"tharushi.k@fixzone.lk", "+94724567890", "pass123", "MANAGER", true, LocalDateTime.now(),
						LocalDateTime.now(), "system", LocalDateTime.now(), "system", "MGR-004", center2Id));
				users.add(new Manager(UUID.fromString("00000000-0000-0000-0000-000000010020"), "Asela Gunaratne",
						"asela.g@fixzone.lk", "+94725678901", "pass123", "MANAGER", true,
						LocalDateTime.now(), LocalDateTime.now(), "system", LocalDateTime.now(), "system", "MGR-005",
						center1Id));

				userRepository.saveAll(users);
				System.out.println("Realistic mock users loaded into database.");

				// Service Centers
				if (serviceCenterRepository.count() == 0) {
					List<ServiceCenter> centers = new ArrayList<>();

					ServiceCenter center1 = new ServiceCenter(
							center1Id,
							owner1,
							"Lanka Express Auto Care",
							"No. 45, Galle Road, Colombo 03",
							"+94112345678",
							"08:00 - 18:00",
							new BigDecimal("4.8"),
							true,
							LocalDateTime.now(), "system", LocalDateTime.now(), "system",
							new String[] { "Toyota", "Honda", "Nissan" });
					centers.add(center1);

					ServiceCenter center2 = new ServiceCenter(
							center2Id,
							owner2,
							"Colombo Precision Service",
							"No. 12, Kandy Road, Kiribathgoda",
							"+94113456789",
							"09:00 - 19:00",
							new BigDecimal("4.5"),
							true,
							LocalDateTime.now(), "system", LocalDateTime.now(), "system",
							new String[] { "BMW", "Mercedes", "Audi" });
					centers.add(center2);

					serviceCenterRepository.saveAll(centers);
					System.out.println("Mock service centers loaded into database.");

					// Service Packages
					if (servicePackageRepository.count() == 0) {
						List<ServicePackage> packages = new ArrayList<>();

						// Packages for Center 1
						packages.add(new ServicePackage(
								UUID.randomUUID(),
								center1,
								"Basic Maintenance",
								"Routine",
								"Oil change, filter replacement, and multi-point inspection.",
								new BigDecimal("4500.00"),
								60,
								true,
								LocalDateTime.now(), "system", LocalDateTime.now(), "system"));

						packages.add(new ServicePackage(
								UUID.randomUUID(),
								center1,
								"Full Service",
								"Comprehensive",
								"Includes basic maintenance plus brake check and fluid top-offs.",
								new BigDecimal("12500.00"),
								120,
								true,
								LocalDateTime.now(), "system", LocalDateTime.now(), "system"));

						// Packages for Center 2
						packages.add(new ServicePackage(
								UUID.randomUUID(),
								center2,
								"Performance Tuning",
								"Specialized",
								"Engine diagnostic and performance optimization.",
								new BigDecimal("25000.00"),
								180,
								true,
								LocalDateTime.now(), "system", LocalDateTime.now(), "system"));

						packages.add(new ServicePackage(
								UUID.randomUUID(),
								center2,
								"Brake System Overhaul",
								"Repair",
								"Replacement of pads, rotors, and brake fluid flush.",
								new BigDecimal("35000.00"),
								240,
								true,
								LocalDateTime.now(), "system", LocalDateTime.now(), "system"));

						servicePackageRepository.saveAll(packages);
						System.out.println("Mock service packages loaded into database.");
					}
				}
			}
		};
	}
}
