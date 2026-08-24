package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.model.Customer;
import com.fixzone.fixzon_backend.model.Vehicle;
import com.fixzone.fixzon_backend.repository.CustomerRepository;
import com.fixzone.fixzon_backend.repository.VehicleRepository;
import com.fixzone.fixzon_backend.service.ImageKitService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/customer")
@Transactional
public class CustomerProfileController {

    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final ImageKitService imageKitService;

    public CustomerProfileController(CustomerRepository customerRepository,
                                      VehicleRepository vehicleRepository,
                                      ImageKitService imageKitService) {
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
        this.imageKitService = imageKitService;
    }

    // --- Profile Endpoints ---

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        Customer customer = customerRepository.findByEmail(authentication.getName()).orElse(null);
        if (customer == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Customer not found"));
        }

        String fullName = customer.getFullName() != null ? customer.getFullName() : "";
        String firstName = "";
        String secondName = "";

        if (fullName.contains(" ")) {
            int firstSpace = fullName.indexOf(" ");
            firstName = fullName.substring(0, firstSpace);
            secondName = fullName.substring(firstSpace + 1);
        } else {
            firstName = fullName;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("firstName", firstName);
        response.put("secondName", secondName);
        response.put("email", customer.getEmail());
        response.put("phoneNumber", customer.getPhone() != null ? customer.getPhone() : "");
        response.put("profilePictureUrl", customer.getProfilePictureUrl() != null ? customer.getProfilePictureUrl() : "");

        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(org.springframework.security.core.Authentication authentication,
                                            @RequestBody Map<String, String> request) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        Customer customer = customerRepository.findByEmail(authentication.getName()).orElse(null);
        if (customer == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Customer not found"));
        }

        String firstName = request.getOrDefault("firstName", "");
        String secondName = request.getOrDefault("secondName", "");
        customer.setFullName((firstName + " " + secondName).trim());
        customer.setPhone(request.get("phoneNumber"));

        if (request.containsKey("profilePictureUrl")) {
            customer.setProfilePictureUrl(request.get("profilePictureUrl"));
        }

        customerRepository.save(customer);
        return getProfile(authentication);
    }

    /**
     * Uploads a profile picture to ImageKit and saves the URL.
     * Expects: { "imageData": "data:image/jpeg;base64,..." }
     */
    @PostMapping("/profile/picture")
    public ResponseEntity<?> uploadProfilePicture(org.springframework.security.core.Authentication authentication,
                                                    @RequestBody Map<String, String> request) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        Customer customer = customerRepository.findByEmail(authentication.getName()).orElse(null);
        if (customer == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Customer not found"));
        }

        String imageData = request.get("imageData");
        if (imageData == null || imageData.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "imageData is required"));
        }

        String imageUrl = imageKitService.uploadImage(imageData, "profile-" + customer.getUserId());
        customer.setProfilePictureUrl(imageUrl);
        customerRepository.save(customer);

        return ResponseEntity.ok(Map.of("profilePictureUrl", imageUrl));
    }

    // --- Vehicle Endpoints ---

    @GetMapping("/vehicles")
    public ResponseEntity<?> getVehicles(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        Customer customer = customerRepository.findByEmail(authentication.getName()).orElse(null);
        if (customer == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Customer not found"));
        }

        return ResponseEntity.ok(vehicleRepository.findByCustomerId(customer.getUserId()));
    }

    @PostMapping("/vehicle")
    public ResponseEntity<?> addVehicle(org.springframework.security.core.Authentication authentication,
                                         @RequestBody Map<String, String> request) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        Customer customer = customerRepository.findByEmail(authentication.getName()).orElse(null);
        if (customer == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Customer not found"));
        }

        String brand = request.get("brand");
        String plateNumber = request.get("plateNumber");
        String vehicleType = request.get("vehicleType"); // e.g. "CAR", "BIKE"
        String model = request.get("model");             // e.g. "Corolla"
        String imageData = request.get("imageData");     // optional base64 image
        String imageUrlReq = request.get("imageUrl");    // optional CDN image URL

        if (brand == null || brand.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Brand is required"));
        }
        if (plateNumber == null || plateNumber.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Plate number is required"));
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setCustomerId(customer.getUserId());
        vehicle.setBrand(brand);
        vehicle.setPlateNumber(plateNumber);
        vehicle.setModel(model != null ? model.trim() : null);
        vehicle.setVehicleType(vehicleType != null ? vehicleType.toUpperCase() : null);
        vehicle.setLastServiceDate(java.time.LocalDate.now());

        // Set vehicle image URL (direct CDN URL or Base64 upload)
        if (imageUrlReq != null && !imageUrlReq.isBlank()) {
            vehicle.setImageUrl(imageUrlReq);
        } else if (imageData != null && !imageData.isBlank()) {
            String imageUrl = imageKitService.uploadImage(imageData, "vehicle-" + plateNumber);
            vehicle.setImageUrl(imageUrl);
        }

        return ResponseEntity.ok(vehicleRepository.save(vehicle));
    }

    /**
     * Updates an existing vehicle's information and/or image.
     */
    @PutMapping("/vehicle/{id}")
    public ResponseEntity<Vehicle> updateVehicle(@PathVariable UUID id,
                                                 org.springframework.security.core.Authentication authentication,
                                                 @RequestBody Map<String, String> request) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        Customer customer = customerRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        if (!vehicle.getCustomerId().equals(customer.getUserId())) {
            return ResponseEntity.status(403).build();
        }

        if (request.containsKey("brand") && request.get("brand") != null) vehicle.setBrand(request.get("brand"));
        if (request.containsKey("model")) vehicle.setModel(request.get("model"));
        if (request.containsKey("plateNumber") && request.get("plateNumber") != null) vehicle.setPlateNumber(request.get("plateNumber"));
        if (request.containsKey("vehicleType") && request.get("vehicleType") != null) vehicle.setVehicleType(request.get("vehicleType").toUpperCase());

        String imageUrlReq = request.get("imageUrl");
        String imageData = request.get("imageData");

        if (imageUrlReq != null && !imageUrlReq.isBlank()) {
            vehicle.setImageUrl(imageUrlReq);
        } else if (imageData != null && !imageData.isBlank()) {
            String imageUrl = imageKitService.uploadImage(imageData, "vehicle-" + vehicle.getPlateNumber());
            vehicle.setImageUrl(imageUrl);
        }

        return ResponseEntity.ok(vehicleRepository.save(vehicle));
    }

    /**
     * Upload or update a vehicle's image using ImageKit.
     */
    @PostMapping("/vehicle/{id}/image")
    public ResponseEntity<?> uploadVehicleImage(@PathVariable UUID id,
                                                 org.springframework.security.core.Authentication authentication,
                                                 @RequestBody Map<String, String> request) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        Vehicle vehicle = vehicleRepository.findById(id).orElse(null);
        if (vehicle == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Vehicle not found"));
        }

        Customer customer = customerRepository.findByEmail(authentication.getName()).orElse(null);
        if (customer == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Customer not found"));
        }

        if (!vehicle.getCustomerId().equals(customer.getUserId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Not your vehicle"));
        }

        String imageUrlReq = request.get("imageUrl");
        String imageData = request.get("imageData");
        String imageUrl = imageUrlReq;

        if (imageUrl == null || imageUrl.isBlank()) {
            if (imageData != null && !imageData.isBlank()) {
                imageUrl = imageKitService.uploadImage(imageData, "vehicle-" + vehicle.getPlateNumber());
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "imageUrl or imageData is required"));
            }
        }

        vehicle.setImageUrl(imageUrl);
        vehicleRepository.save(vehicle);

        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }

    @DeleteMapping("/vehicle/{id}")
    public ResponseEntity<?> deleteVehicle(@PathVariable UUID id,
                                            org.springframework.security.core.Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        Vehicle vehicle = vehicleRepository.findById(id).orElse(null);
        if (vehicle == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Vehicle not found"));
        }

        Customer customer = customerRepository.findByEmail(authentication.getName()).orElse(null);
        if (customer == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Customer not found"));
        }

        if (!vehicle.getCustomerId().equals(customer.getUserId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Not your vehicle"));
        }

        vehicleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- Settings Endpoints ---

    @GetMapping("/settings")
    public ResponseEntity<?> getSettings(org.springframework.security.core.Authentication authentication) {
        Map<String, Object> settings = new HashMap<>();
        settings.put("language", "English");
        return ResponseEntity.ok(settings);
    }

    @PutMapping("/settings")
    public ResponseEntity<?> updateSettings(org.springframework.security.core.Authentication authentication,
                                             @RequestBody Map<String, Object> settings) {
        return ResponseEntity.ok(settings);
    }
}
