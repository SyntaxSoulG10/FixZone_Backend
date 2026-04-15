package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.PaymentRecordDTO;
import com.fixzone.fixzon_backend.model.PaymentRecord;
import com.fixzone.fixzon_backend.repository.PaymentRecordRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentRecordService {

    private final PaymentRecordRepository paymentRecordRepository;

    public PaymentRecordService(PaymentRecordRepository paymentRecordRepository) {
        this.paymentRecordRepository = paymentRecordRepository;
    }

    public List<PaymentRecordDTO> getAllPayments() {
        return paymentRecordRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public PaymentRecordDTO getPaymentById(UUID id) {
        Objects.requireNonNull(id, "ID must not be null");
        PaymentRecord payment = paymentRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment record not found with id: " + id));
        return convertToDTO(payment);
    }

    public List<PaymentRecordDTO> getPaymentsByInvoice(UUID invoiceId) {
        return paymentRecordRepository.findByInvoiceId(invoiceId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<PaymentRecordDTO> getPaymentsByCenter(UUID centerId) {
        return paymentRecordRepository.findByCenterId(centerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<PaymentRecordDTO> getPaymentsByStatus(String status) {
        return paymentRecordRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<PaymentRecordDTO> getPaymentsByMethod(String method) {
        return paymentRecordRepository.findByMethod(method).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public PaymentRecordDTO createPayment(PaymentRecordDTO dto) {
        PaymentRecord payment = convertToEntity(dto);
        if (payment.getPaymentId() == null) {
            payment.setPaymentId(UUID.randomUUID());
        }
        return convertToDTO(paymentRecordRepository.save(payment));
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

        return convertToDTO(Objects.requireNonNull(paymentRecordRepository.save(existing)));
    }

    public void deletePayment(UUID id) {
        Objects.requireNonNull(id, "ID must not be null");
        paymentRecordRepository.deleteById(id);
    }

    private PaymentRecordDTO convertToDTO(PaymentRecord payment) {
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
                payment.getUpdatedBy()
        );
    }

    private PaymentRecord convertToEntity(PaymentRecordDTO dto) {
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
