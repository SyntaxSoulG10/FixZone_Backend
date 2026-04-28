package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.PaymentRecordDTO;
import com.fixzone.fixzon_backend.model.PaymentRecord;
import com.fixzone.fixzon_backend.repository.PaymentRecordRepository;
import com.fixzone.fixzon_backend.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentRecordService {

    private final PaymentRecordRepository paymentRecordRepository;
    private final InvoiceRepository invoiceRepository;

    public PaymentRecordService(PaymentRecordRepository paymentRecordRepository, InvoiceRepository invoiceRepository) {
        this.paymentRecordRepository = paymentRecordRepository;
        this.invoiceRepository = invoiceRepository;
    }

    public List<PaymentRecordDTO> getAllPayments() {
        try {
            // Transformation to DTO secures hidden columns from HTTP exposure.
            return paymentRecordRepository.findAll().stream()
                    .map(this::transformToDataTransferObject)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Database error while retrieving payments: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve payments", e);
        }
    }

    public List<PaymentRecordDTO> getPaymentsByCompanyCode(String companyCode) {
        if (companyCode == null || companyCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Company code must not be null or empty");
        }
        try {
            // Find all invoices for this company first
            List<com.fixzone.fixzon_backend.model.Invoice> invoices = invoiceRepository.findByCompanyCode(companyCode);
            List<PaymentRecordDTO> allPayments = new ArrayList<>();

            for (com.fixzone.fixzon_backend.model.Invoice inv : invoices) {
                allPayments.addAll(paymentRecordRepository.findByInvoiceId(inv.getInvoiceId()).stream()
                        .map(this::transformToDataTransferObject)
                        .collect(Collectors.toList()));
            }
            return allPayments;
        } catch (Exception e) {
            System.err.println("Database error while retrieving payments by company code: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve payments by company code", e);
        }
    }

    public PaymentRecordDTO getPaymentById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
        try {
            PaymentRecord payment = paymentRecordRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Payment record not found with id: " + id));
            return transformToDataTransferObject(payment);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Database error while retrieving payment by ID: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve payment by ID", e);
        }
    }

    public List<PaymentRecordDTO> getPaymentsByInvoice(UUID invoiceId) {
        if (invoiceId == null) {
            throw new IllegalArgumentException("Invoice ID must not be null");
        }
        try {
            return paymentRecordRepository.findByInvoiceId(invoiceId).stream()
                    .map(this::transformToDataTransferObject)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Database error while retrieving payments by invoice: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve payments by invoice", e);
        }
    }

    public List<PaymentRecordDTO> getPaymentsByCenter(UUID centerId) {
        if (centerId == null) {
            throw new IllegalArgumentException("Center ID must not be null");
        }
        try {
            return paymentRecordRepository.findByCenterId(centerId).stream()
                    .map(this::transformToDataTransferObject)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Database error while retrieving payments by center: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve payments by center", e);
        }
    }

    public List<PaymentRecordDTO> getPaymentsByStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status must not be null or empty");
        }
        try {
            return paymentRecordRepository.findByStatus(status).stream()
                    .map(this::transformToDataTransferObject)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Database error while retrieving payments by status: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve payments by status", e);
        }
    }

    public List<PaymentRecordDTO> getPaymentsByMethod(String method) {
        if (method == null || method.trim().isEmpty()) {
            throw new IllegalArgumentException("Method must not be null or empty");
        }
        try {
            return paymentRecordRepository.findByMethod(method).stream()
                    .map(this::transformToDataTransferObject)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Database error while retrieving payments by method: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve payments by method", e);
        }
    }

    public PaymentRecordDTO createPayment(PaymentRecordDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Payment record data must not be null");
        }
        try {
            PaymentRecord payment = transformToDatabaseEntity(dto);
            if (payment.getPaymentId() == null) {
                payment.setPaymentId(UUID.randomUUID());
            }
            return transformToDataTransferObject(paymentRecordRepository.save(payment));
        } catch (Exception e) {
            System.err.println("Database error while creating payment: " + e.getMessage());
            throw new RuntimeException("Failed to create payment", e);
        }
    }

    public PaymentRecordDTO updatePayment(UUID id, PaymentRecordDTO dto) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Payment data must not be null");
        }
        try {
            PaymentRecord existing = paymentRecordRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Payment record not found with id: " + id));

            existing.setInvoiceId(dto.getInvoiceId());
            existing.setCenterId(dto.getCenterId());
            existing.setAmount(dto.getAmount());
            existing.setMethod(dto.getMethod());
            existing.setProviderTransactionId(dto.getProviderTransactionId());
            existing.setStatus(dto.getStatus());
            existing.setProcessedAt(dto.getProcessedAt());
            existing.setUpdatedBy(dto.getUpdatedBy());

            PaymentRecord saved = paymentRecordRepository.save(existing);
            return transformToDataTransferObject(saved);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Database error while updating payment: " + e.getMessage());
            throw new RuntimeException("Failed to update payment", e);
        }
    }

    public void deletePayment(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
        try {
            if (!paymentRecordRepository.existsById(id)) {
                throw new IllegalStateException("Payment record not found with id: " + id);
            }
            paymentRecordRepository.deleteById(id);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Database error while deleting payment: " + e.getMessage());
            throw new RuntimeException("Failed to delete payment", e);
        }
    }

    // Direct constructor mapping for reliable type transfer mapping.
    private PaymentRecordDTO transformToDataTransferObject(PaymentRecord payment) {
        if (payment == null) {
            throw new IllegalArgumentException("PaymentRecord must not be null");
        }
        return new PaymentRecordDTO(
                payment.getPaymentId(),
                payment.getInvoiceId(),
                payment.getCenterId(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getProviderTransactionId(),
                payment.getStatus(),
                payment.getProcessedAt(),
                payment.getCreatedAt(),
                payment.getCreatedBy(),
                payment.getUpdatedAt(),
                payment.getUpdatedBy());
    }

    private PaymentRecord transformToDatabaseEntity(PaymentRecordDTO dto) {
        PaymentRecord payment = new PaymentRecord();
        payment.setPaymentId(dto.getPaymentId());
        payment.setInvoiceId(dto.getInvoiceId());
        payment.setCenterId(dto.getCenterId());
        payment.setAmount(dto.getAmount());
        payment.setMethod(dto.getMethod());
        payment.setProviderTransactionId(dto.getProviderTransactionId());
        payment.setStatus(dto.getStatus());
        payment.setProcessedAt(dto.getProcessedAt());
        payment.setCreatedBy(dto.getCreatedBy());
        payment.setUpdatedBy(dto.getUpdatedBy());
        return payment;
    }
}
