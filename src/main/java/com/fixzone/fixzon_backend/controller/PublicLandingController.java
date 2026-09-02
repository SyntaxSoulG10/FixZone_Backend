package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.model.ServicePackage;
import com.fixzone.fixzon_backend.model.SubscriptionPlan;
import com.fixzone.fixzon_backend.repository.BookingRepository;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.ServicePackageRepository;
import com.fixzone.fixzon_backend.repository.UserRepository;
import com.fixzone.fixzon_backend.service.SubscriptionPlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicLandingController {

    private final UserRepository userRepository;
    private final ServiceCenterRepository serviceCenterRepository;
    private final BookingRepository bookingRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final SubscriptionPlanService subscriptionPlanService;

    public PublicLandingController(
            UserRepository userRepository,
            ServiceCenterRepository serviceCenterRepository,
            BookingRepository bookingRepository,
            ServicePackageRepository servicePackageRepository,
            SubscriptionPlanService subscriptionPlanService) {
        this.userRepository = userRepository;
        this.serviceCenterRepository = serviceCenterRepository;
        this.bookingRepository = bookingRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.subscriptionPlanService = subscriptionPlanService;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getLandingStats() {
        long usersCount = userRepository.count();
        long centersCount = serviceCenterRepository.count();
        long bookingsCount = bookingRepository.count();
        long packagesCount = servicePackageRepository.count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("activeUsers", usersCount);
        stats.put("registeredCenters", centersCount);
        stats.put("servicesCompleted", bookingsCount);
        stats.put("totalPackages", packagesCount);
        stats.put("satisfactionRate", "99.4%");

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/packages")
    public ResponseEntity<List<ServicePackage>> getFeaturedPackages() {
        List<ServicePackage> packages = servicePackageRepository.findByIsActiveTrue();
        if (packages.size() > 6) {
            packages = packages.subList(0, 6);
        }
        return ResponseEntity.ok(packages);
    }

    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlan>> getPublicPlans() {
        return ResponseEntity.ok(subscriptionPlanService.getAllPlans());
    }
}
