package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.PaymentRecordDTO;
import com.fixzone.fixzon_backend.service.PaymentRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment-records")
@CrossOrigin("*")
public class PaymentRecordController {

    // Construct injection over field injection guarantees dependencies are met securely
    private final PaymentRecordService paymentRecordService;

    public PaymentRecordController(PaymentRecordService paymentRecordService) {
        this.paymentRecordService = paymentRecordService;
    }

    @GetMapping
    public ResponseEntity<List<PaymentRecordDTO>> getAllPayments() {
        return ResponseEntity.ok(paymentRecordService.getAllPayments());
    }

    @GetMapping("/current")
    public ResponseEntity<List<PaymentRecordDTO>> getCurrentOwnerPayments() {
        // Hardcoded for development
        return ResponseEntity.ok(paymentRecordService.getPaymentsByCompanyCode("FIX001"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentRecordDTO> getPaymentById(@PathVariable UUID id) {
        PaymentRecordDTO payment = paymentRecordService.getPaymentById(id);
        return payment != null ? ResponseEntity.ok(payment) : ResponseEntity.notFound().build();
    }

    @GetMapping("/invoice/{invoiceId}")
    public ResponseEntity<List<PaymentRecordDTO>> getPaymentsByInvoice(@PathVariable UUID invoiceId) {
        return ResponseEntity.ok(paymentRecordService.getPaymentsByInvoice(invoiceId));
    }

    @GetMapping("/center/{centerId}")
    public ResponseEntity<List<PaymentRecordDTO>> getPaymentsByCenter(@PathVariable UUID centerId) {
        return ResponseEntity.ok(paymentRecordService.getPaymentsByCenter(centerId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<PaymentRecordDTO>> getPaymentsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(paymentRecordService.getPaymentsByStatus(status));
    }

    @GetMapping("/method/{method}")
    public ResponseEntity<List<PaymentRecordDTO>> getPaymentsByMethod(@PathVariable String method) {
        return ResponseEntity.ok(paymentRecordService.getPaymentsByMethod(method));
    }

    @PostMapping
    public ResponseEntity<PaymentRecordDTO> createPayment(@RequestBody PaymentRecordDTO dto) {
        // Enforce 201 Created explicitly signaling creation resolution
        return ResponseEntity.status(201).body(paymentRecordService.createPayment(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentRecordDTO> updatePayment(@PathVariable UUID id,
            @RequestBody PaymentRecordDTO dto) {
        PaymentRecordDTO updatedPayment = paymentRecordService.updatePayment(id, dto);
        return updatedPayment != null ? ResponseEntity.ok(updatedPayment) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable UUID id) {
        paymentRecordService.deletePayment(id);
        // Dispatch explicit 204 meaning the resource is securely omitted.
        return ResponseEntity.noContent().build();
    }
}
