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

    private final CustomerRepository customerRepository;
    private final ServiceCenterRepository serviceCenterRepository;
    private final OwnerRepository ownerRepository;

    public CustomerService(CustomerRepository customerRepository, 
                           ServiceCenterRepository serviceCenterRepository, 
                           OwnerRepository ownerRepository) {
        this.customerRepository = customerRepository;
        this.serviceCenterRepository = serviceCenterRepository;
        this.ownerRepository = ownerRepository;
    }

    public List<CustomerDTO> getAllCustomers() {
        try {
            return customerRepository.findAll().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Database error while retrieving customers: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve customers", e);
        }
    }

    public List<CustomerDTO> getCustomersByOwnerCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner code cannot be null or empty");
        }
        try {
            return ownerRepository.findByOwnerCode(code)
                    .map(owner -> {
                        List<UUID> centerIds = serviceCenterRepository.findByOwner_UserId(owner.getUserId())
                                .stream()
                                .map(com.fixzone.fixzon_backend.model.ServiceCenter::getCenterId)
                                .collect(Collectors.toList());
                        if (centerIds.isEmpty()) return List.<CustomerDTO>of();
                        return customerRepository.findCustomersByCenterIds(centerIds).stream()
                                .map(this::convertToDTO)
                                .collect(Collectors.toList());
                    })
                    .orElse(List.of());
        } catch (Exception e) {
            System.err.println("Database error while retrieving customers by owner code: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve customers by owner code", e);
        }
    }

    private CustomerDTO convertToDTO(Customer customer) {
        if (customer == null) return null;
        CustomerDTO dto = new CustomerDTO();
        BeanUtils.copyProperties(customer, dto);
        return dto;
    }
}
