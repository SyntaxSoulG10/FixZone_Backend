package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.InvoiceDTO;
import com.fixzone.fixzon_backend.service.InvoiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@CrossOrigin("*")
public class InvoiceController {

    // Utilize explicit constructor injection to resolve immutable service boundaries cleanly
    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public ResponseEntity<List<InvoiceDTO>> getAllInvoices() {
        return ResponseEntity.ok(invoiceService.getAllInvoices());
    }

    @GetMapping("/current")
    public ResponseEntity<List<InvoiceDTO>> getCurrentOwnerInvoices() {
        // Hardcoded for development
        return ResponseEntity.ok(invoiceService.getInvoicesByCompanyCode("FIX001"));
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

    @GetMapping("/status/{status}")
    public ResponseEntity<List<InvoiceDTO>> getInvoicesByStatus(@PathVariable String status) {
        return ResponseEntity.ok(invoiceService.getInvoicesByStatus(status));
    }

    @GetMapping("/company/{companyCode}")
    public ResponseEntity<List<InvoiceDTO>> getInvoicesByCompanyCode(@PathVariable String companyCode) {
        return ResponseEntity.ok(invoiceService.getInvoicesByCompanyCode(companyCode));
    }

    @PostMapping
    public ResponseEntity<InvoiceDTO> createInvoice(@RequestBody InvoiceDTO dto) {
        // Enforce 201 Created REST convention explicitly
        return ResponseEntity.status(201).body(invoiceService.createInvoice(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InvoiceDTO> updateInvoice(@PathVariable UUID id,
            @RequestBody InvoiceDTO dto) {
        InvoiceDTO updatedInvoice = invoiceService.updateInvoice(id, dto);
        return updatedInvoice != null ? ResponseEntity.ok(updatedInvoice) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable UUID id) {
        invoiceService.deleteInvoice(id);
        // Dispatch explicit 204 meaning resource handled correctly and removed.
        return ResponseEntity.noContent().build();
    }
}
