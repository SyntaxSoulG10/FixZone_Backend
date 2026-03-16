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


    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public ResponseEntity<List<InvoiceDTO>> getAllInvoices() {
        return ResponseEntity.ok(invoiceService.getAllInvoices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDTO> getInvoiceById(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
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
        return ResponseEntity.ok(invoiceService.createInvoice(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InvoiceDTO> updateInvoice(@PathVariable UUID id,
            @RequestBody InvoiceDTO dto) {
        return ResponseEntity.ok(invoiceService.updateInvoice(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable UUID id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }
}
