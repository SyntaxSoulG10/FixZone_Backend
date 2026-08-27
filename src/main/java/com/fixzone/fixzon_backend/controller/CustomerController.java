package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.CustomerDTO;
import com.fixzone.fixzon_backend.service.CustomerService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.fixzone.fixzon_backend.DTO.ServiceCenterDTO;
import com.fixzone.fixzon_backend.middleware.RequireRole;
import com.fixzone.fixzon_backend.service.ServiceCenterService;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import com.fixzone.fixzon_backend.service.OwnerService;
import com.fixzone.fixzon_backend.DTO.OwnerDTO;


@RestController
@RequestMapping("/api/customers")
@RequireRole({"ROLE_SUPER_ADMIN", "ROLE_OWNER", "ROLE_SERVICE_MANAGER", "ROLE_CUSTOMER", "CUSTOMER", "COMPANY_OWNER", "OWNER", "ROLE_COMPANY_OWNER"})
public class CustomerController {

    private final CustomerService customerService;
    private final OwnerService ownerService;
    private final ServiceCenterService serviceCenterService;

    public CustomerController(CustomerService customerService, OwnerService ownerService, ServiceCenterService serviceCenterService) {
        this.customerService = customerService;
        this.ownerService = ownerService;
        this.serviceCenterService = serviceCenterService;
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
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return ResponseEntity.ok(List.of());
        }

        String email = auth.getName();

        // Retrieve the owner to get their ownerCode
        OwnerDTO owner = ownerService.retrieveOwnerByEmail(email);
        if (owner == null) {
            return ResponseEntity.ok(List.of());
        }

        return ResponseEntity.ok(customerService.getCustomersByOwnerCode(owner.getOwnerCode()));
    }

    /**
     * Retrieves service centers where the customer has completed at least one booking.
     * This provides the "Trusted Service Centers" data directly to the frontend.
     */
    @GetMapping("/{customerId}/trusted-service-centers")
    public ResponseEntity<List<ServiceCenterDTO>> getTrustedServiceCenters(@PathVariable UUID customerId) {
        return ResponseEntity.ok(serviceCenterService.getTrustedCentersForCustomer(customerId));
    }
}
