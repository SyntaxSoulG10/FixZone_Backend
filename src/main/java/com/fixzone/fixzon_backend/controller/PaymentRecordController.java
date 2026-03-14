package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.PaymentRecordDTO;
import com.fixzone.fixzon_backend.service.PaymentRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment-records")
public class PaymentRecordController {

    private final PaymentRecordService paymentRecordService;

    public PaymentRecordController(PaymentRecordService paymentRecordService) {
        this.paymentRecordService = paymentRecordService;
    }

    @GetMapping
    public ResponseEntity<List<PaymentRecordDTO>> getAllPayments() {
        return ResponseEntity.ok(paymentRecordService.getAllPayments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentRecordDTO> getPaymentById(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentRecordService.getPaymentById(id));
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
        return ResponseEntity.ok(paymentRecordService.createPayment(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentRecordDTO> updatePayment(@PathVariable UUID id,
            @RequestBody PaymentRecordDTO dto) {
        return ResponseEntity.ok(paymentRecordService.updatePayment(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable UUID id) {
        paymentRecordService.deletePayment(id);
        return ResponseEntity.noContent().build();
    }
}
