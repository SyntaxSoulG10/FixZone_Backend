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

@RestController
@RequestMapping("/api/customers")
@CrossOrigin("*")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

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
            // Hardcoded for development
            return ResponseEntity.ok(customerService.getCustomersByOwnerCode("FIX001"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch current customers: " + e.getMessage());
        }
    }
}
