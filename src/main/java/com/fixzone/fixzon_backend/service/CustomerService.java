package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.CustomerDTO;
import com.fixzone.fixzon_backend.model.Customer;
import com.fixzone.fixzon_backend.repository.CustomerRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ServiceCenterRepository serviceCenterRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    public List<CustomerDTO> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<CustomerDTO> getCustomersByOwnerCode(String code) {
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
    }

    private CustomerDTO convertToDTO(Customer customer) {
        if (customer == null) return null;
        CustomerDTO dto = new CustomerDTO();
        BeanUtils.copyProperties(customer, dto);
        return dto;
    }
}
