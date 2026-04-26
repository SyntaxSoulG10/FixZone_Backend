package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.CustomerDTO;
import com.fixzone.fixzon_backend.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import com.fixzone.fixzon_backend.service.OwnerService;
import com.fixzone.fixzon_backend.DTO.OwnerDTO;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin("*")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private OwnerService ownerService;

    @GetMapping
    public ResponseEntity<List<CustomerDTO>> getAllCustomers() {
        try {
            return ResponseEntity.ok(customerService.getAllCustomers());
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch customers: " + e.getMessage());
        }
    }

    @GetMapping("/current")
    public ResponseEntity<List<CustomerDTO>> getCurrentOwnerCustomers() {
        try {
            // Get the current authenticated user's email from the SecurityContext
            String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            
            // Retrieve the owner to get their ownerCode
            OwnerDTO owner = ownerService.retrieveOwnerByEmail(email);
            if (owner == null) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).build();
            }

            return ResponseEntity.ok(customerService.getCustomersByOwnerCode(owner.getOwnerCode()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch current customers: " + e.getMessage());
        }
    }
}
