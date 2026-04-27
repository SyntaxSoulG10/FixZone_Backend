package com.fixzone.fixzon_backend.config;

import com.fixzone.fixzon_backend.model.Owner;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class DevDataSeeder {

    @Bean
    CommandLineRunner initDatabase(ServiceCenterRepository repository, OwnerRepository ownerRepository) {
        return args -> {
            // Check if we already have pending requests to avoid duplicates on restart
            if (repository.findByStatus("PENDING").isEmpty()) {
                // Find any existing owner or create a temporary one for the test data
                Owner owner = ownerRepository.findAll().stream()
                        .findFirst()
                        .orElseGet(() -> {
                            Owner o = new Owner();
                            o.setUserId(UUID.randomUUID());
                            o.setFullName("Platform Test Owner");
                            o.setEmail("test-owner@fixzone.com");
                            o.setRole("ROLE_COMPANY_OWNER");
                            o.setPasswordHash("dummy-hash");
                            o.setOwnerCode("OWN-TEST-001");
                            o.setCompanyName("FixZone Partner Network");
                            return ownerRepository.save(o);
                        });

                // Add 1st Pending Station
                ServiceCenter sc1 = new ServiceCenter();
                sc1.setCenterId(UUID.randomUUID());
                sc1.setName("AutoFix Pro Station");
                sc1.setAddress("123 Tech Avenue, Colombo");
                sc1.setContactPhone("+94 77 123 4567");
                sc1.setStatus("PENDING");
                sc1.setIsActive(false);
                sc1.setOwner(owner);
                repository.save(sc1);

                // Add 2nd Pending Station
                ServiceCenter sc2 = new ServiceCenter();
                sc2.setCenterId(UUID.randomUUID());
                sc2.setName("Elite Car Care");
                sc2.setAddress("45 Galle Road, Mount Lavinia");
                sc2.setContactPhone("+94 11 987 6543");
                sc2.setStatus("PENDING");
                sc2.setIsActive(false);
                sc2.setOwner(owner);
                repository.save(sc2);
                
                System.out.println("DEBUG: Seeded 2 pending service center requests for development/testing.");
            }
        };
    }
}
