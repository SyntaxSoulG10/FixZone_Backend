package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.CustomerDTO;
import com.fixzone.fixzon_backend.model.Customer;
import com.fixzone.fixzon_backend.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import org.springframework.stereotype.Service;

import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
import com.fixzone.fixzon_backend.repository.BookingRepository;
import com.fixzone.fixzon_backend.repository.InvoiceRepository;
import com.fixzone.fixzon_backend.model.Invoice;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomerService {
    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final ServiceCenterRepository serviceCenterRepository;
    private final OwnerRepository ownerRepository;
    private final BookingRepository bookingRepository;
    private final InvoiceRepository invoiceRepository;

    public CustomerService(CustomerRepository customerRepository, 
                           ServiceCenterRepository serviceCenterRepository, 
                           OwnerRepository ownerRepository,
                           BookingRepository bookingRepository,
                           InvoiceRepository invoiceRepository) {
        this.customerRepository = customerRepository;
        this.serviceCenterRepository = serviceCenterRepository;
        this.ownerRepository = ownerRepository;
        this.bookingRepository = bookingRepository;
        this.invoiceRepository = invoiceRepository;
    }

    public List<CustomerDTO> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
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
                                .map(customer -> {
                                    CustomerDTO dto = convertToDTO(customer);
                                    
                                    // Sum only the paid invoices for this customer at the owner's service centers
                                    BigDecimal tenantTotalSpent = invoiceRepository.findByIssuedToCustomerId(customer.getUserId()).stream()
                                            .filter(i -> centerIds.contains(i.getCenterId()) && "PAID".equalsIgnoreCase(i.getStatus()))
                                            .map(Invoice::getTotal)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                                    dto.setTotalSpent(tenantTotalSpent);

                                    // Count only the bookings for this customer at the owner's service centers
                                    long tenantVisits = bookingRepository.findByCustomerId(customer.getUserId()).stream()
                                            .filter(b -> centerIds.contains(b.getCenterId()))
                                            .count();
                                    
                                    // If visits is 0 but they spent money, make it at least 1
                                    if (tenantVisits == 0 && tenantTotalSpent.compareTo(BigDecimal.ZERO) > 0) {
                                        tenantVisits = 1;
                                    }
                                    dto.setVisits((int) tenantVisits);

                                    return dto;
                                })
                                .collect(Collectors.toList());
                    })
                    .orElse(List.of());
        } catch (Exception e) {
            log.error("Database error while retrieving customers by owner code: {}", e.getMessage(), e);
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
