package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.customerprofile.ApiMessageResponse;
import com.fixzone.fixzon_backend.DTO.customerprofile.CreateVehicleRequest;
import com.fixzone.fixzon_backend.DTO.customerprofile.CustomerProfileResponse;
import com.fixzone.fixzon_backend.DTO.customerprofile.CustomerSettingsResponse;
import com.fixzone.fixzon_backend.DTO.customerprofile.UpdateCustomerProfileRequest;
import com.fixzone.fixzon_backend.DTO.customerprofile.UpdateCustomerSettingsRequest;
import com.fixzone.fixzon_backend.DTO.customerprofile.VehicleResponse;
import com.fixzone.fixzon_backend.service.customerprofile.CustomerProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
@CrossOrigin("*")
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    public CustomerProfileController(CustomerProfileService customerProfileService) {
        this.customerProfileService = customerProfileService;
    }

    @GetMapping("/profile")
    public ResponseEntity<CustomerProfileResponse> getProfile() {
        return ResponseEntity.ok(customerProfileService.getProfile());
    }

    @PutMapping("/profile")
    public ResponseEntity<CustomerProfileResponse> updateProfile(@Valid @RequestBody UpdateCustomerProfileRequest request) {
        return ResponseEntity.ok(customerProfileService.updateProfile(request));
    }

    @PostMapping("/vehicle")
    public ResponseEntity<VehicleResponse> addVehicle(@Valid @RequestBody CreateVehicleRequest request) {
        return ResponseEntity.ok(customerProfileService.addVehicle(request));
    }

    @GetMapping("/vehicles")
    public ResponseEntity<List<VehicleResponse>> getVehicles() {
        return ResponseEntity.ok(customerProfileService.getVehicles());
    }

    @DeleteMapping("/vehicle/{id}")
    public ResponseEntity<ApiMessageResponse> deleteVehicle(@PathVariable Long id) {
        customerProfileService.deleteVehicle(id);
        return ResponseEntity.ok(new ApiMessageResponse("Vehicle deleted successfully"));
    }

    @GetMapping("/settings")
    public ResponseEntity<CustomerSettingsResponse> getSettings() {
        return ResponseEntity.ok(customerProfileService.getSettings());
    }

    @PutMapping("/settings")
    public ResponseEntity<CustomerSettingsResponse> updateSettings(@Valid @RequestBody UpdateCustomerSettingsRequest request) {
        return ResponseEntity.ok(customerProfileService.updateSettings(request));
    }
}
