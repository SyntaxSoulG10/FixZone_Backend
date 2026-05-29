package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.CustomerDTO;
import com.fixzone.fixzon_backend.service.CustomerService;

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
public class CustomerController {

    private final CustomerService customerService;
    private final OwnerService ownerService;

    public CustomerController(CustomerService customerService, OwnerService ownerService) {
        this.customerService = customerService;
        this.ownerService = ownerService;
    }

    /**
     * Provides a list of all registered customers across the platform.
     * Intended for high-level administrative overviews and cross-tenant auditing by Super Admins.
     */
    @GetMapping
    public ResponseEntity<List<CustomerDTO>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    /**
     * Filters and returns only the customers belonging to the authenticated business owner's centers.
     * This implements multi-tenant data isolation, ensuring owners only interact with their own client base.
     */
    @GetMapping("/current")
    public ResponseEntity<List<CustomerDTO>> getCurrentOwnerCustomers() {
        // Get the current authenticated user's email from the SecurityContext
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Retrieve the owner to get their ownerCode
        OwnerDTO owner = ownerService.retrieveOwnerByEmail(email);
        if (owner == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(customerService.getCustomersByOwnerCode(owner.getOwnerCode()));
    }
}
