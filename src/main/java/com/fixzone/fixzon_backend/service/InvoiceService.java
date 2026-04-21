package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.InvoiceDTO;
import com.fixzone.fixzon_backend.model.Invoice;
import com.fixzone.fixzon_backend.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public List<InvoiceDTO> getAllInvoices() {
        return invoiceRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public InvoiceDTO getInvoiceById(UUID id) {
        Objects.requireNonNull(id, "ID must not be null");
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));
        return convertToDTO(invoice);
    }

    public InvoiceDTO getInvoiceByBooking(UUID bookingId) {
        Invoice invoice = invoiceRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Invoice not found for booking: " + bookingId));
        return convertToDTO(invoice);
    }

    public List<InvoiceDTO> getInvoicesByCenter(UUID centerId) {
        return invoiceRepository.findByCenterId(centerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<InvoiceDTO> getInvoicesByCustomer(UUID customerId) {
        return invoiceRepository.findByIssuedToCustomerId(customerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<InvoiceDTO> getInvoicesByStatus(String status) {
        return invoiceRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<InvoiceDTO> getInvoicesByCompanyCode(String companyCode) {
        return invoiceRepository.findByCompanyCode(companyCode).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public InvoiceDTO createInvoice(InvoiceDTO dto) {
        Invoice invoice = convertToEntity(dto);
        if (invoice.getInvoiceId() == null) {
            invoice.setInvoiceId(UUID.randomUUID());
        }
        return convertToDTO(Objects.requireNonNull(invoiceRepository.save(invoice)));
    }

    public InvoiceDTO updateInvoice(UUID id, InvoiceDTO dto) {
        Objects.requireNonNull(id, "ID must not be null");
        Invoice existing = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));
        
        if (dto != null) {
            existing.setCompanyCode(dto.getCompanyCode());
            existing.setCenterId(dto.getCenterId());
            existing.setBookingId(dto.getBookingId());
            existing.setIssuedToCustomerId(dto.getIssuedToCustomerId());
            existing.setSubtotal(dto.getSubtotal());
            existing.setTax(dto.getTax());
            existing.setDiscount(dto.getDiscount());
            existing.setTotal(dto.getTotal());
            existing.setStatus(dto.getStatus());
            existing.setIssuedAt(dto.getIssuedAt());
            existing.setDueAt(dto.getDueAt());
            existing.setUpdatedBy(dto.getUpdatedBy());
        }

        @SuppressWarnings("null")
        Invoice saved = invoiceRepository.save(existing);
        return convertToDTO(saved);
    }

    public void deleteInvoice(UUID id) {
        Objects.requireNonNull(id, "ID must not be null");
        invoiceRepository.deleteById(id);
    }

    private InvoiceDTO convertToDTO(Invoice invoice) {
        Objects.requireNonNull(invoice, "Invoice must not be null");
        return new InvoiceDTO(
                invoice.getInvoiceId(),
                invoice.getCompanyCode(),
                invoice.getCenterId(),
                invoice.getBookingId(),
                invoice.getIssuedToCustomerId(),
                invoice.getSubtotal(),
                invoice.getTax(),
                invoice.getDiscount(),
                invoice.getTotal(),
                invoice.getStatus(),
                invoice.getIssuedAt(),
                invoice.getDueAt(),
                invoice.getCreatedAt(),
                invoice.getCreatedBy(),
                invoice.getUpdatedAt(),
                invoice.getUpdatedBy()
        );
    }

    private Invoice convertToEntity(InvoiceDTO dto) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(dto.getInvoiceId());
        invoice.setCompanyCode(dto.getCompanyCode());
        invoice.setCenterId(dto.getCenterId());
        invoice.setBookingId(dto.getBookingId());
        invoice.setIssuedToCustomerId(dto.getIssuedToCustomerId());
        invoice.setSubtotal(dto.getSubtotal());
        invoice.setTax(dto.getTax());
        invoice.setDiscount(dto.getDiscount());
        invoice.setTotal(dto.getTotal());
        invoice.setStatus(dto.getStatus());
        invoice.setIssuedAt(dto.getIssuedAt());
        invoice.setDueAt(dto.getDueAt());
        invoice.setCreatedBy(dto.getCreatedBy());
        invoice.setUpdatedBy(dto.getUpdatedBy());
        return invoice;
    }
}
