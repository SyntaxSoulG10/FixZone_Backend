package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.InvoiceDTO;
import com.fixzone.fixzon_backend.model.Customer;
import com.fixzone.fixzon_backend.model.Invoice;
import com.fixzone.fixzon_backend.repository.CustomerRepository;
import com.fixzone.fixzon_backend.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    private UUID invoiceId;
    private UUID centerId;
    private UUID bookingId;
    private UUID customerId;
    private String companyCode;
    private Invoice sampleInvoice;

    @BeforeEach
    void setUp() {
        invoiceId = UUID.randomUUID();
        centerId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        companyCode = "COMP123";

        sampleInvoice = new Invoice();
        sampleInvoice.setInvoiceId(invoiceId);
        sampleInvoice.setCompanyCode(companyCode);
        sampleInvoice.setCenterId(centerId);
        sampleInvoice.setBookingId(bookingId);
        sampleInvoice.setIssuedToCustomerId(customerId);
        sampleInvoice.setSubtotal(new BigDecimal("1500.00"));
        sampleInvoice.setTax(new BigDecimal("150.00"));
        sampleInvoice.setDiscount(new BigDecimal("100.00"));
        sampleInvoice.setTotal(new BigDecimal("1550.00"));
        sampleInvoice.setStatus("ISSUED");
        sampleInvoice.setIssuedAt(LocalDateTime.now());
        sampleInvoice.setDueAt(LocalDateTime.now().plusDays(7));
    }

    @Test
    void getAllInvoices_ShouldReturnInvoiceDTOList() {
        when(invoiceRepository.findAll()).thenReturn(Collections.singletonList(sampleInvoice));

        List<InvoiceDTO> result = invoiceService.getAllInvoices();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInvoiceId()).isEqualTo(invoiceId);
        verify(invoiceRepository, times(1)).findAll();
    }

    @Test
    void getInvoiceById_ShouldReturnInvoiceDTO_WhenFound() {
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(sampleInvoice));

        InvoiceDTO result = invoiceService.getInvoiceById(invoiceId);

        assertThat(result).isNotNull();
        assertThat(result.getInvoiceId()).isEqualTo(invoiceId);
        verify(invoiceRepository, times(1)).findById(invoiceId);
    }

    @Test
    void getInvoiceById_ShouldThrowException_WhenNotFound() {
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getInvoiceById(invoiceId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invoice not found with id");
    }

    @Test
    void getInvoiceById_ShouldThrowException_WhenIdIsNull() {
        assertThatThrownBy(() -> invoiceService.getInvoiceById(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ID must not be null");
    }

    @Test
    void getInvoiceByBooking_ShouldReturnInvoiceDTO_WhenFound() {
        when(invoiceRepository.findByBookingId(bookingId)).thenReturn(Optional.of(sampleInvoice));

        InvoiceDTO result = invoiceService.getInvoiceByBooking(bookingId);

        assertThat(result).isNotNull();
        assertThat(result.getBookingId()).isEqualTo(bookingId);
        verify(invoiceRepository, times(1)).findByBookingId(bookingId);
    }

    @Test
    void getInvoiceByBooking_ShouldThrowException_WhenNotFound() {
        when(invoiceRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getInvoiceByBooking(bookingId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invoice not found for booking");
    }

    @Test
    void getInvoiceByBooking_ShouldThrowException_WhenBookingIdIsNull() {
        assertThatThrownBy(() -> invoiceService.getInvoiceByBooking(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Booking ID must not be null");
    }

    @Test
    void getInvoicesByCenter_ShouldReturnInvoiceDTOList() {
        when(invoiceRepository.findByCenterId(centerId)).thenReturn(Collections.singletonList(sampleInvoice));

        List<InvoiceDTO> result = invoiceService.getInvoicesByCenter(centerId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCenterId()).isEqualTo(centerId);
        verify(invoiceRepository, times(1)).findByCenterId(centerId);
    }

    @Test
    void getInvoicesByCenter_ShouldThrowException_WhenCenterIdIsNull() {
        assertThatThrownBy(() -> invoiceService.getInvoicesByCenter(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Center ID must not be null");
    }

    @Test
    void getInvoicesByCustomer_ShouldReturnInvoiceDTOList() {
        when(invoiceRepository.findByIssuedToCustomerId(customerId)).thenReturn(Collections.singletonList(sampleInvoice));

        List<InvoiceDTO> result = invoiceService.getInvoicesByCustomer(customerId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIssuedToCustomerId()).isEqualTo(customerId);
        verify(invoiceRepository, times(1)).findByIssuedToCustomerId(customerId);
    }

    @Test
    void getInvoicesByCustomer_ShouldThrowException_WhenCustomerIdIsNull() {
        assertThatThrownBy(() -> invoiceService.getInvoicesByCustomer(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer ID must not be null");
    }

    @Test
    void getInvoicesForCurrentCustomer_ShouldReturnInvoiceDTOList_WhenEmailIsValid() {
        String email = "test@customer.com";
        Customer customer = new Customer();
        customer.setUserId(customerId);
        customer.setEmail(email);

        when(customerRepository.findByEmail(email)).thenReturn(Optional.of(customer));
        when(invoiceRepository.findByIssuedToCustomerId(customerId)).thenReturn(Collections.singletonList(sampleInvoice));

        List<InvoiceDTO> result = invoiceService.getInvoicesForCurrentCustomer(email);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIssuedToCustomerId()).isEqualTo(customerId);
        verify(customerRepository, times(1)).findByEmail(email);
        verify(invoiceRepository, times(1)).findByIssuedToCustomerId(customerId);
    }

    @Test
    void getInvoicesForCurrentCustomer_ShouldThrowException_WhenEmailIsNull() {
        assertThatThrownBy(() -> invoiceService.getInvoicesForCurrentCustomer(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email must not be null");
    }

    @Test
    void getInvoicesForCurrentCustomer_ShouldThrowException_WhenCustomerNotFound() {
        String email = "unknown@customer.com";
        when(customerRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getInvoicesForCurrentCustomer(email))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Customer not found");
    }

    @Test
    void getInvoicesByStatus_ShouldReturnInvoiceDTOList() {
        String status = "ISSUED";
        when(invoiceRepository.findByStatus(status)).thenReturn(Collections.singletonList(sampleInvoice));

        List<InvoiceDTO> result = invoiceService.getInvoicesByStatus(status);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(status);
        verify(invoiceRepository, times(1)).findByStatus(status);
    }

    @Test
    void getInvoicesByStatus_ShouldThrowException_WhenStatusIsEmpty() {
        assertThatThrownBy(() -> invoiceService.getInvoicesByStatus("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Status must not be null or empty");
    }

    @Test
    void getInvoicesByCompanyCode_ShouldReturnInvoiceDTOList() {
        when(invoiceRepository.findByCompanyCode(companyCode)).thenReturn(Collections.singletonList(sampleInvoice));

        List<InvoiceDTO> result = invoiceService.getInvoicesByCompanyCode(companyCode);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCompanyCode()).isEqualTo(companyCode);
        verify(invoiceRepository, times(1)).findByCompanyCode(companyCode);
    }

    @Test
    void getInvoicesByCompanyCode_ShouldThrowException_WhenCompanyCodeIsEmpty() {
        assertThatThrownBy(() -> invoiceService.getInvoicesByCompanyCode(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Company code must not be null or empty");
    }

    @Test
    void createInvoice_ShouldSaveAndReturnDTO() {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setCompanyCode(companyCode);
        dto.setCenterId(centerId);
        dto.setBookingId(bookingId);
        dto.setIssuedToCustomerId(customerId);
        dto.setSubtotal(new BigDecimal("1500.00"));
        dto.setTax(new BigDecimal("150.00"));
        dto.setDiscount(new BigDecimal("100.00"));
        dto.setTotal(new BigDecimal("1550.00"));
        dto.setStatus("ISSUED");

        when(invoiceRepository.save(any(Invoice.class))).thenReturn(sampleInvoice);

        InvoiceDTO result = invoiceService.createInvoice(dto);

        assertThat(result).isNotNull();
        assertThat(result.getInvoiceId()).isEqualTo(invoiceId);
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }

    @Test
    void createInvoice_ShouldThrowException_WhenDTOIsNull() {
        assertThatThrownBy(() -> invoiceService.createInvoice(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invoice data must not be null");
    }

    @Test
    void updateInvoice_ShouldModifyAndSaveInvoice() {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setCompanyCode("NEWCODE");
        dto.setCenterId(centerId);
        dto.setBookingId(bookingId);
        dto.setIssuedToCustomerId(customerId);
        dto.setSubtotal(new BigDecimal("2000.00"));
        dto.setTax(new BigDecimal("200.00"));
        dto.setDiscount(new BigDecimal("200.00"));
        dto.setTotal(new BigDecimal("2000.00"));
        dto.setStatus("PAID");

        Invoice updatedInvoice = new Invoice();
        updatedInvoice.setInvoiceId(invoiceId);
        updatedInvoice.setCompanyCode("NEWCODE");
        updatedInvoice.setCenterId(centerId);
        updatedInvoice.setBookingId(bookingId);
        updatedInvoice.setIssuedToCustomerId(customerId);
        updatedInvoice.setSubtotal(new BigDecimal("2000.00"));
        updatedInvoice.setTax(new BigDecimal("200.00"));
        updatedInvoice.setDiscount(new BigDecimal("200.00"));
        updatedInvoice.setTotal(new BigDecimal("2000.00"));
        updatedInvoice.setStatus("PAID");

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(sampleInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(updatedInvoice);

        InvoiceDTO result = invoiceService.updateInvoice(invoiceId, dto);

        assertThat(result).isNotNull();
        assertThat(result.getCompanyCode()).isEqualTo("NEWCODE");
        assertThat(result.getStatus()).isEqualTo("PAID");
        verify(invoiceRepository, times(1)).findById(invoiceId);
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }

    @Test
    void updateInvoice_ShouldThrowException_WhenNotFound() {
        InvoiceDTO dto = new InvoiceDTO();
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.updateInvoice(invoiceId, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invoice not found with id");
    }

    @Test
    void deleteInvoice_ShouldCallRepositoryDelete_WhenInvoiceExists() {
        when(invoiceRepository.existsById(invoiceId)).thenReturn(true);

        invoiceService.deleteInvoice(invoiceId);

        verify(invoiceRepository, times(1)).existsById(invoiceId);
        verify(invoiceRepository, times(1)).deleteById(invoiceId);
    }

    @Test
    void deleteInvoice_ShouldThrowException_WhenInvoiceDoesNotExist() {
        when(invoiceRepository.existsById(invoiceId)).thenReturn(false);

        assertThatThrownBy(() -> invoiceService.deleteInvoice(invoiceId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invoice not found with id");
    }
}
