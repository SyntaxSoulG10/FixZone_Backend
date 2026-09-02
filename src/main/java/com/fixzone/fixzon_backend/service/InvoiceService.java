package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.InvoiceDTO;
import com.fixzone.fixzon_backend.model.Invoice;
import com.fixzone.fixzon_backend.enums.BookingStatus;
import com.fixzone.fixzon_backend.repository.BookingRepository;
import com.fixzone.fixzon_backend.repository.CustomerRepository;
import com.fixzone.fixzon_backend.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import java.util.List;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;

    public InvoiceService(InvoiceRepository invoiceRepository, CustomerRepository customerRepository, BookingRepository bookingRepository) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<InvoiceDTO> getAllInvoices() {
        // Transformation to DTO secures hidden columns from HTTP exposure.
        return invoiceRepository.findAll().stream()
                .map(this::transformToDataTransferObject)
                .collect(Collectors.toList());
    }

    public InvoiceDTO getInvoiceById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
        return invoiceRepository.findById(id)
                .map(this::transformToDataTransferObject)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));
    }

    public InvoiceDTO getInvoiceByBooking(UUID bookingId) {
        if (bookingId == null) {
            throw new IllegalArgumentException("Booking ID must not be null");
        }
        return invoiceRepository.findByBookingId(bookingId)
                .map(this::transformToDataTransferObject)
                .orElseThrow(() -> new RuntimeException("Invoice not found for booking: " + bookingId));
    }

    public List<InvoiceDTO> getInvoicesByCenter(UUID centerId) {
        if (centerId == null) {
            throw new IllegalArgumentException("Center ID must not be null");
        }
        return invoiceRepository.findByCenterId(centerId).stream()
                .map(this::transformToDataTransferObject)
                .collect(Collectors.toList());
    }

    public List<InvoiceDTO> getInvoicesByCustomer(UUID customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID must not be null");
        }
        return invoiceRepository.findByIssuedToCustomerId(customerId).stream()
                .map(this::transformToDataTransferObject)
                .collect(Collectors.toList());
    }

    /**
     * Resolves the logged-in customer by email and returns their invoices.
     * Used by the customer dashboard invoice download feature.
     */
    public List<InvoiceDTO> getInvoicesForCurrentCustomer(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be null");
        }
        UUID customerId = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + email))
                .getUserId();
        return getInvoicesByCustomer(customerId);
    }

    public List<InvoiceDTO> getInvoicesByStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status must not be null or empty");
        }
        return invoiceRepository.findByStatus(status).stream()
                .map(this::transformToDataTransferObject)
                .collect(Collectors.toList());
    }

    public List<InvoiceDTO> getInvoicesByCompanyCode(String companyCode) {
        if (companyCode == null || companyCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Company code must not be null or empty");
        }
        return invoiceRepository.findByCompanyCode(companyCode).stream()
                .map(this::transformToDataTransferObject)
                .collect(Collectors.toList());
    }

    public InvoiceDTO createInvoice(InvoiceDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Invoice data must not be null");
        }
        Invoice invoice = transformToDatabaseEntity(dto);
        if (invoice.getInvoiceId() == null) {
            invoice.setInvoiceId(UUID.randomUUID());
        }
        if (invoice.getIssuedAt() == null) {
            invoice.setIssuedAt(java.time.LocalDateTime.now());
        }
        Invoice savedInvoice = invoiceRepository.save(invoice);
        if ("PAID".equalsIgnoreCase(savedInvoice.getStatus()) && savedInvoice.getBookingId() != null) {
            bookingRepository.findById(savedInvoice.getBookingId()).ifPresent(booking -> {
                booking.setStatus(BookingStatus.PAID);
                bookingRepository.save(booking);
            });
        }
        return transformToDataTransferObject(savedInvoice);
    }

    public InvoiceDTO updateInvoice(UUID id, InvoiceDTO dto) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Invoice data must not be null");
        }
        
        Invoice existing = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));

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

        Invoice saved = invoiceRepository.save(existing);
        if ("PAID".equalsIgnoreCase(saved.getStatus()) && saved.getBookingId() != null) {
            bookingRepository.findById(saved.getBookingId()).ifPresent(booking -> {
                booking.setStatus(BookingStatus.PAID);
                bookingRepository.save(booking);
            });
        }
        return transformToDataTransferObject(saved);
    }

    public InvoiceDTO updateInvoiceStatus(UUID id, String status) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));

        invoice.setStatus(status);
        Invoice saved = invoiceRepository.save(invoice);

        if ("PAID".equalsIgnoreCase(status) && saved.getBookingId() != null) {
            bookingRepository.findById(saved.getBookingId()).ifPresent(booking -> {
                booking.setStatus(BookingStatus.PAID);
                bookingRepository.save(booking);
            });
        }

        return transformToDataTransferObject(saved);
    }

    public void deleteInvoice(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
        if (!invoiceRepository.existsById(id)) {
            throw new IllegalStateException("Invoice not found with id: " + id);
        }
        invoiceRepository.deleteById(id);
    }

    // Direct constructor mapping for reliable type transfer mapping.
    private InvoiceDTO transformToDataTransferObject(Invoice invoice) {
        if (invoice == null) {
            throw new IllegalArgumentException("Invoice must not be null");
        }
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
                invoice.getUpdatedBy());
    }

    private Invoice transformToDatabaseEntity(InvoiceDTO dto) {
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
