package com.fixzone.fixzon_backend;

import com.fixzone.fixzon_backend.model.Customer;
import com.fixzone.fixzon_backend.repository.CustomerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootApplication
public class FixzonBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(FixzonBackendApplication.class, args);
	}

	@Bean
	public CommandLineRunner dataLoader(CustomerRepository customerRepository) {
		return args -> {
			if (customerRepository.count() == 0) {
				List<Customer> customers = List.of(
						new Customer(null, "John Doe", "john.doe@example.com", 12, new BigDecimal("4500.00"),
								LocalDateTime.now().minusDays(2), "VIP", "https://i.pravatar.cc/150?u=1"),
						new Customer(null, "Sarah Smith", "sarah.s@example.com", 8, new BigDecimal("2100.00"),
								LocalDateTime.now().minusDays(5), "Regular", "https://i.pravatar.cc/150?u=2"),
						new Customer(null, "Michael Brown", "m.brown@test.com", 15, new BigDecimal("5200.00"),
								LocalDateTime.now().minusWeeks(1), "VIP", "https://i.pravatar.cc/150?u=3"),
						new Customer(null, "Emily Davis", "emily.d@example.com", 3, new BigDecimal("450.00"),
								LocalDateTime.now().minusWeeks(2), "New", "https://i.pravatar.cc/150?u=4"),
						new Customer(null, "David Wilson", "david.w@example.com", 6, new BigDecimal("1200.00"),
								LocalDateTime.now().minusMonths(1), "Regular", "https://i.pravatar.cc/150?u=5"));
				customerRepository.saveAll(customers);
				System.out.println("Mock customers loaded into database.");
			}
		};
	}

}
