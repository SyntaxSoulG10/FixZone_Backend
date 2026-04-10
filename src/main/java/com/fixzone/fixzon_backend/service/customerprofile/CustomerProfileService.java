package com.fixzone.fixzon_backend.service.customerprofile;

import com.fixzone.fixzon_backend.DTO.customerprofile.CreateVehicleRequest;
import com.fixzone.fixzon_backend.DTO.customerprofile.CustomerProfileResponse;
import com.fixzone.fixzon_backend.DTO.customerprofile.CustomerSettingsResponse;
import com.fixzone.fixzon_backend.DTO.customerprofile.UpdateCustomerProfileRequest;
import com.fixzone.fixzon_backend.DTO.customerprofile.UpdateCustomerSettingsRequest;
import com.fixzone.fixzon_backend.DTO.customerprofile.VehicleResponse;
import com.fixzone.fixzon_backend.entity.Customer;
import com.fixzone.fixzon_backend.entity.Settings;
import com.fixzone.fixzon_backend.entity.Vehicle;
import com.fixzone.fixzon_backend.repository.CustomerProfileRepository;
import com.fixzone.fixzon_backend.repository.SettingsRepository;
import com.fixzone.fixzon_backend.repository.VehicleRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerProfileService {

    private static final Long MOCK_CUSTOMER_ID = 1L;

    private final CustomerProfileRepository customerProfileRepository;
    private final VehicleRepository vehicleRepository;
    private final SettingsRepository settingsRepository;

    public CustomerProfileService(
            CustomerProfileRepository customerProfileRepository,
            VehicleRepository vehicleRepository,
            SettingsRepository settingsRepository
    ) {
        this.customerProfileRepository = customerProfileRepository;
        this.vehicleRepository = vehicleRepository;
        this.settingsRepository = settingsRepository;
    }

    public CustomerProfileResponse getProfile() {
        Customer customer = getOrCreateCustomer();
        return toProfileResponse(customer);
    }

    @Transactional
    public CustomerProfileResponse updateProfile(UpdateCustomerProfileRequest request) {
        Customer customer = getOrCreateCustomer();
        customer.setName(request.getName().trim());
        customer.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        customer.setProfileImageUrl(request.getProfileImageUrl() != null ? request.getProfileImageUrl().trim() : null);
        Customer saved = customerProfileRepository.save(customer);
        return toProfileResponse(saved);
    }

    @Transactional
    public VehicleResponse addVehicle(CreateVehicleRequest request) {
        Vehicle vehicle = new Vehicle();
        vehicle.setCustomerId(MOCK_CUSTOMER_ID);
        vehicle.setBrand(request.getBrand().trim());
        vehicle.setModel(request.getModel().trim());
        vehicle.setPlateNumber(request.getPlateNumber().trim());
        Vehicle saved = vehicleRepository.save(vehicle);
        return toVehicleResponse(saved);
    }

    public List<VehicleResponse> getVehicles() {
        return vehicleRepository.findByCustomerId(MOCK_CUSTOMER_ID)
                .stream()
                .map(this::toVehicleResponse)
                .toList();
    }

    @Transactional
    public void deleteVehicle(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findByIdAndCustomerId(vehicleId, MOCK_CUSTOMER_ID)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found for current customer"));
        vehicleRepository.delete(vehicle);
    }

    public CustomerSettingsResponse getSettings() {
        Settings settings = getOrCreateSettings();
        return toSettingsResponse(settings);
    }

    @Transactional
    public CustomerSettingsResponse updateSettings(UpdateCustomerSettingsRequest request) {
        Settings settings = getOrCreateSettings();
        settings.setLanguage(request.getLanguage().trim());
        settings.setNotificationsEnabled(request.getNotificationsEnabled());
        Settings saved = settingsRepository.save(settings);
        return toSettingsResponse(saved);
    }

    private Customer getOrCreateCustomer() {
        return customerProfileRepository.findByCustomerId(MOCK_CUSTOMER_ID)
                .orElseGet(() -> {
                    Customer customer = new Customer();
                    customer.setCustomerId(MOCK_CUSTOMER_ID);
                    customer.setName("Demo Customer");
                    customer.setEmail("customer1@fixzone.com");
                    customer.setPhone("");
                    customer.setProfileImageUrl("");
                    return customerProfileRepository.save(customer);
                });
    }

    private Settings getOrCreateSettings() {
        return settingsRepository.findByCustomerId(MOCK_CUSTOMER_ID)
                .orElseGet(() -> {
                    Settings settings = new Settings();
                    settings.setCustomerId(MOCK_CUSTOMER_ID);
                    settings.setLanguage("en");
                    settings.setNotificationsEnabled(true);
                    return settingsRepository.save(settings);
                });
    }

    private CustomerProfileResponse toProfileResponse(Customer customer) {
        return new CustomerProfileResponse(
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getProfileImageUrl()
        );
    }

    private VehicleResponse toVehicleResponse(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getPlateNumber()
        );
    }

    private CustomerSettingsResponse toSettingsResponse(Settings settings) {
        return new CustomerSettingsResponse(
                settings.getLanguage(),
                settings.isNotificationsEnabled()
        );
    }
}
