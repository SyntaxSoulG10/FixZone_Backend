package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.InvoiceDTO;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
import com.fixzone.fixzon_backend.service.InvoiceService;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final OwnerRepository ownerRepository;

    public InvoiceController(InvoiceService invoiceService, OwnerRepository ownerRepository) {
        this.invoiceService = invoiceService;
        this.ownerRepository = ownerRepository;
    }

    @GetMapping
    public ResponseEntity<List<InvoiceDTO>> getAllInvoices() {
        return ResponseEntity.ok(invoiceService.getAllInvoices());
    }

    @GetMapping("/current")
    public ResponseEntity<List<InvoiceDTO>> getCurrentOwnerInvoices(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        return ownerRepository.findByEmail(authentication.getName())
                .map(owner -> ResponseEntity.ok(invoiceService.getInvoicesByCompanyCode(owner.getOwnerCode())))
                .orElse(ResponseEntity.status(404).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDTO> getInvoiceById(@PathVariable UUID id) {
        InvoiceDTO invoice = invoiceService.getInvoiceById(id);
        return invoice != null ? ResponseEntity.ok(invoice) : ResponseEntity.notFound().build();
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<InvoiceDTO> getInvoiceByBooking(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(invoiceService.getInvoiceByBooking(bookingId));
    }

    @GetMapping("/center/{centerId}")
    public ResponseEntity<List<InvoiceDTO>> getInvoicesByCenter(@PathVariable UUID centerId) {
        return ResponseEntity.ok(invoiceService.getInvoicesByCenter(centerId));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<InvoiceDTO>> getInvoicesByCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(invoiceService.getInvoicesByCustomer(customerId));
    }

    /**
     * Returns invoices for the currently authenticated customer.
     * Used by the customer dashboard "download invoice" button on completed bookings.
     */
    @GetMapping("/my")
    public ResponseEntity<List<InvoiceDTO>> getMyInvoices(Authentication authentication) {
        return ResponseEntity.ok(invoiceService.getInvoicesForCurrentCustomer(authentication.getName()));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<InvoiceDTO>> getInvoicesByStatus(@PathVariable String status) {
        return ResponseEntity.ok(invoiceService.getInvoicesByStatus(status));
    }

    @GetMapping("/company/{companyCode}")
    public ResponseEntity<List<InvoiceDTO>> getInvoicesByCompanyCode(@PathVariable String companyCode) {
        return ResponseEntity.ok(invoiceService.getInvoicesByCompanyCode(companyCode));
    }

    @PostMapping
    public ResponseEntity<InvoiceDTO> createInvoice(@jakarta.validation.Valid @RequestBody InvoiceDTO dto) {
        return ResponseEntity.status(201).body(invoiceService.createInvoice(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InvoiceDTO> updateInvoice(@PathVariable UUID id,
            @jakarta.validation.Valid @RequestBody InvoiceDTO dto) {
        InvoiceDTO updatedInvoice = invoiceService.updateInvoice(id, dto);
        return updatedInvoice != null ? ResponseEntity.ok(updatedInvoice) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable UUID id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }
}
