package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.model.Customer;
import com.fixzone.fixzon_backend.model.Vehicle;
import com.fixzone.fixzon_backend.repository.CustomerRepository;
import com.fixzone.fixzon_backend.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/customer")
@CrossOrigin("*")
public class CustomerProfileController {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    // --- Profile Endpoints ---

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Principal principal) {
        Customer customer = customerRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        String fullName = customer.getFullName();
        String firstName = "";
        String secondName = "";
        
        if (fullName != null && fullName.contains(" ")) {
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
        response.put("phoneNumber", customer.getPhone());
        response.put("profilePictureUrl", customer.getProfilePictureUrl());
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(Principal principal, @RequestBody Map<String, String> request) {
        Customer customer = customerRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        String firstName = request.getOrDefault("firstName", "");
        String secondName = request.getOrDefault("secondName", "");
        customer.setFullName((firstName + " " + secondName).trim());
        customer.setPhone(request.get("phoneNumber"));
        
        // Handle profile picture URL update
        if (request.containsKey("profilePictureUrl")) {
            customer.setProfilePictureUrl(request.get("profilePictureUrl"));
        }
        
        customerRepository.save(customer);
        return getProfile(principal);
    }

    // --- Vehicle Endpoints ---

    @GetMapping("/vehicles")
    public ResponseEntity<List<Vehicle>> getVehicles(Principal principal) {
        Customer customer = customerRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        return ResponseEntity.ok(vehicleRepository.findByCustomerId(customer.getUserId()));
    }

    @PostMapping("/vehicle")
    public ResponseEntity<Vehicle> addVehicle(Principal principal, @RequestBody Vehicle vehicle) {
        Customer customer = customerRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        vehicle.setCustomerId(customer.getUserId());
        return ResponseEntity.ok(vehicleRepository.save(vehicle));
    }

    @DeleteMapping("/vehicle/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable UUID id) {
        java.util.Objects.requireNonNull(id, "Vehicle ID must not be null");
        vehicleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- Settings Endpoints (Mocked for now as we don't have settings table) ---

    @GetMapping("/settings")
    public ResponseEntity<?> getSettings(Principal principal) {
        Map<String, Object> settings = new HashMap<>();
        settings.put("notificationsOn", true);
        settings.put("language", "English");
        return ResponseEntity.ok(settings);
    }

    @PutMapping("/settings")
    public ResponseEntity<?> updateSettings(Principal principal, @RequestBody Map<String, Object> settings) {
        // Just return what was sent to simulate success
        return ResponseEntity.ok(settings);
    }

    // --- Payment Methods Endpoints (Mocked) ---

    @GetMapping("/payment-methods")
    public ResponseEntity<?> getPaymentMethods(Principal principal) {
        List<Map<String, Object>> methods = new ArrayList<>();
        Map<String, Object> card = new HashMap<>();
        card.put("id", 1);
        card.put("cardType", "Visa");
        card.put("lastFour", "4242");
        card.put("brandColor", "bg-blue-600");
        methods.add(card);
        return ResponseEntity.ok(methods);
    }

    @PostMapping("/payment-method")
    public ResponseEntity<?> addPaymentMethod(Principal principal, @RequestBody Map<String, Object> payload) {
        payload.put("id", new Random().nextInt(1000));
        return ResponseEntity.ok(payload);
    }

    @DeleteMapping("/payment-method/{id}")
    public ResponseEntity<Void> deletePaymentMethod(@PathVariable Long id) {
        return ResponseEntity.noContent().build();
    }
}
