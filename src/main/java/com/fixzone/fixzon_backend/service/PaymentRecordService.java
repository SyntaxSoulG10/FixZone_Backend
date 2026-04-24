package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.PaymentRecordDTO;
import com.fixzone.fixzon_backend.model.PaymentRecord;
import com.fixzone.fixzon_backend.repository.PaymentRecordRepository;
import com.fixzone.fixzon_backend.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
        // Encapsulating database entities via transformations secures hidden columns
        // from HTTP exposure
        return paymentRecordRepository.findAll().stream()
                .map(this::transformToDataTransferObject)
                .collect(Collectors.toList());
    }

    public List<PaymentRecordDTO> getPaymentsByCompanyCode(String companyCode) {
        // Find all invoices for this company first
        List<com.fixzone.fixzon_backend.model.Invoice> invoices = invoiceRepository.findByCompanyCode(companyCode);
        List<PaymentRecordDTO> allPayments = new ArrayList<>();

        for (com.fixzone.fixzon_backend.model.Invoice inv : invoices) {
            allPayments.addAll(paymentRecordRepository.findByInvoiceId(inv.getInvoiceId()).stream()
                    .map(this::transformToDataTransferObject)
                    .collect(Collectors.toList()));
        }
        return allPayments;
    }

    public PaymentRecordDTO getPaymentById(UUID id) {
        Objects.requireNonNull(id, "ID must not be null");
        PaymentRecord payment = paymentRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment record not found with id: " + id));
        return transformToDataTransferObject(payment);
    }

    public List<PaymentRecordDTO> getPaymentsByInvoice(UUID invoiceId) {
        return paymentRecordRepository.findByInvoiceId(invoiceId).stream()
                .map(this::transformToDataTransferObject)
                .collect(Collectors.toList());
    }

    public List<PaymentRecordDTO> getPaymentsByCenter(UUID centerId) {
        return paymentRecordRepository.findByCenterId(centerId).stream()
                .map(this::transformToDataTransferObject)
                .collect(Collectors.toList());
    }

    public List<PaymentRecordDTO> getPaymentsByStatus(String status) {
        return paymentRecordRepository.findByStatus(status).stream()
                .map(this::transformToDataTransferObject)
                .collect(Collectors.toList());
    }

    public List<PaymentRecordDTO> getPaymentsByMethod(String method) {
        return paymentRecordRepository.findByMethod(method).stream()
                .map(this::transformToDataTransferObject)
                .collect(Collectors.toList());
    }

    public PaymentRecordDTO createPayment(PaymentRecordDTO dto) {
        PaymentRecord payment = transformToDatabaseEntity(dto);
        if (payment.getPaymentId() == null) {
            payment.setPaymentId(UUID.randomUUID());
        }
        return transformToDataTransferObject(paymentRecordRepository.save(payment));
    }

    public PaymentRecordDTO updatePayment(UUID id, PaymentRecordDTO dto) {
        Objects.requireNonNull(id, "ID must not be null");
        PaymentRecord existing = paymentRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment record not found with id: " + id));

        if (dto != null) {
            existing.setInvoiceId(dto.getInvoiceId());
            existing.setCenterId(dto.getCenterId());
            existing.setAmount(dto.getAmount());
            existing.setMethod(dto.getMethod());
            existing.setProviderTransactionId(dto.getProviderTransactionId());
            existing.setStatus(dto.getStatus());
            existing.setProcessedAt(dto.getProcessedAt());
            existing.setUpdatedBy(dto.getUpdatedBy());
        }

        @SuppressWarnings("null")
        PaymentRecord saved = paymentRecordRepository.save(existing);
        return transformToDataTransferObject(Objects.requireNonNull(saved));
    }

    public void deletePayment(UUID id) {
        Objects.requireNonNull(id, "ID must not be null");
        paymentRecordRepository.deleteById(id);
    }

    // Direct constructor mapping enforces strict type transfer mapping reliably
    private PaymentRecordDTO transformToDataTransferObject(PaymentRecord payment) {
        Objects.requireNonNull(payment, "PaymentRecord must not be null");
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
