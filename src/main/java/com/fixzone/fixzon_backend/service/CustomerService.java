package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.CustomerDTO;
import com.fixzone.fixzon_backend.model.Customer;
import com.fixzone.fixzon_backend.repository.CustomerRepository;
import org.springframework.beans.BeanUtils;

import org.springframework.stereotype.Service;

import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    // Repository for customer database operations
    private final CustomerRepository customerRepository;
    
    // Repository for service center lookups
    private final ServiceCenterRepository serviceCenterRepository;
    
    // Repository for owner lookups
    private final OwnerRepository ownerRepository;

    // Constructor-based dependency injection
    public CustomerService(CustomerRepository customerRepository, 
                           ServiceCenterRepository serviceCenterRepository, 
                           OwnerRepository ownerRepository) {
        this.customerRepository = customerRepository;
        this.serviceCenterRepository = serviceCenterRepository;
        this.ownerRepository = ownerRepository;
    }

    /**
     * Retrieves all customers from the database
     * @return List of all customer DTOs
     * @throws RuntimeException if database error occurs
     */
    public List<CustomerDTO> getAllCustomers() {
        try {
            // Fetch all customers and convert to DTOs
            return customerRepository.findAll().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Database error while retrieving customers: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve customers", e);
        }
    }

    /**
     * Retrieves all customers associated with a specific owner
     * Finds service centers owned by the owner, then finds customers at those centers
     * @param code Unique owner code identifier
     * @return List of customer DTOs for the owner's centers, empty list if no owners found
     * @throws IllegalArgumentException if code is null or empty
     * @throws RuntimeException if database error occurs
     */
    public List<CustomerDTO> getCustomersByOwnerCode(String code) {
        // Validate owner code parameter
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner code cannot be null or empty");
        }
        try {
            // Step 1: Find owner by owner code
            return ownerRepository.findByOwnerCode(code)
                    .map(owner -> {
                        // Step 2: Find all service centers owned by this owner
                        List<UUID> centerIds = serviceCenterRepository.findByOwner_UserId(owner.getUserId())
                                .stream()
                                .map(com.fixzone.fixzon_backend.model.ServiceCenter::getCenterId)
                                .collect(Collectors.toList());
                        
                        // If owner has no service centers, return empty list
                        if (centerIds.isEmpty()) return List.<CustomerDTO>of();
                        
                        // Step 3: Find all customers at these service centers
                        return customerRepository.findCustomersByCenterIds(centerIds).stream()
                                .map(this::convertToDTO)
                                .collect(Collectors.toList());
                    })
                    .orElse(List.of()); // Return empty list if owner not found
        } catch (Exception e) {
            System.err.println("Database error while retrieving customers by owner code: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve customers by owner code", e);
        }
    }

    /**
     * Converts Customer entity to CustomerDTO
     * @param customer Customer entity from database
     * @return CustomerDTO with all customer information
     */
    private CustomerDTO convertToDTO(Customer customer) {
        // Return null if customer is null
        if (customer == null) return null;
        
        // Create DTO and copy all properties
        CustomerDTO dto = new CustomerDTO();
        BeanUtils.copyProperties(customer, dto);
        return dto;
    }
}
