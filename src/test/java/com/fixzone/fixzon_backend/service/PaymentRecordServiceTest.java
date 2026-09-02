package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.PaymentRecordDTO;
import com.fixzone.fixzon_backend.model.Invoice;
import com.fixzone.fixzon_backend.model.PaymentRecord;
import com.fixzone.fixzon_backend.repository.InvoiceRepository;
import com.fixzone.fixzon_backend.repository.PaymentRecordRepository;
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
class PaymentRecordServiceTest {

    @Mock
    private PaymentRecordRepository paymentRecordRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private PaymentRecordService paymentRecordService;

    private UUID paymentId;
    private UUID invoiceId;
    private UUID centerId;
    private PaymentRecord sampleRecord;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID();
        invoiceId = UUID.randomUUID();
        centerId = UUID.randomUUID();

        sampleRecord = new PaymentRecord();
        sampleRecord.setPaymentId(paymentId);
        sampleRecord.setInvoiceId(invoiceId);
        sampleRecord.setCenterId(centerId);
        sampleRecord.setAmount(new BigDecimal("1550.00"));
        sampleRecord.setMethod("CARD");
        sampleRecord.setProviderTransactionId("txn_test_123");
        sampleRecord.setStatus("SUCCESS");
        sampleRecord.setProcessedAt(LocalDateTime.now());
    }

    @Test
    void getAllPayments_ShouldReturnPaymentRecordDTOList() {
        when(paymentRecordRepository.findAll()).thenReturn(Collections.singletonList(sampleRecord));

        List<PaymentRecordDTO> result = paymentRecordService.getAllPayments();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPaymentId()).isEqualTo(paymentId);
        verify(paymentRecordRepository, times(1)).findAll();
    }

    @Test
    void getPaymentsByCompanyCode_ShouldReturnPaymentRecordDTOList() {
        String companyCode = "COMP123";
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(invoiceId);
        invoice.setCompanyCode(companyCode);

        when(invoiceRepository.findByCompanyCode(companyCode)).thenReturn(Collections.singletonList(invoice));
        when(paymentRecordRepository.findByInvoiceId(invoiceId)).thenReturn(Collections.singletonList(sampleRecord));

        List<PaymentRecordDTO> result = paymentRecordService.getPaymentsByCompanyCode(companyCode);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPaymentId()).isEqualTo(paymentId);
        verify(invoiceRepository, times(1)).findByCompanyCode(companyCode);
        verify(paymentRecordRepository, times(1)).findByInvoiceId(invoiceId);
    }

    @Test
    void getPaymentsByCompanyCode_ShouldThrowException_WhenCompanyCodeIsEmpty() {
        assertThatThrownBy(() -> paymentRecordService.getPaymentsByCompanyCode("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Company code must not be null or empty");
    }

    @Test
    void getPaymentById_ShouldReturnPaymentRecordDTO_WhenFound() {
        when(paymentRecordRepository.findById(paymentId)).thenReturn(Optional.of(sampleRecord));

        PaymentRecordDTO result = paymentRecordService.getPaymentById(paymentId);

        assertThat(result).isNotNull();
        assertThat(result.getPaymentId()).isEqualTo(paymentId);
        verify(paymentRecordRepository, times(1)).findById(paymentId);
    }

    @Test
    void getPaymentById_ShouldThrowException_WhenNotFound() {
        when(paymentRecordRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentRecordService.getPaymentById(paymentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Payment record not found with id");
    }

    @Test
    void getPaymentById_ShouldThrowException_WhenIdIsNull() {
        assertThatThrownBy(() -> paymentRecordService.getPaymentById(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ID must not be null");
    }

    @Test
    void getPaymentsByInvoice_ShouldReturnPaymentRecordDTOList() {
        when(paymentRecordRepository.findByInvoiceId(invoiceId)).thenReturn(Collections.singletonList(sampleRecord));

        List<PaymentRecordDTO> result = paymentRecordService.getPaymentsByInvoice(invoiceId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInvoiceId()).isEqualTo(invoiceId);
        verify(paymentRecordRepository, times(1)).findByInvoiceId(invoiceId);
    }

    @Test
    void getPaymentsByInvoice_ShouldThrowException_WhenInvoiceIdIsNull() {
        assertThatThrownBy(() -> paymentRecordService.getPaymentsByInvoice(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invoice ID must not be null");
    }

    @Test
    void getPaymentsByCenter_ShouldReturnPaymentRecordDTOList() {
        when(paymentRecordRepository.findByCenterId(centerId)).thenReturn(Collections.singletonList(sampleRecord));

        List<PaymentRecordDTO> result = paymentRecordService.getPaymentsByCenter(centerId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCenterId()).isEqualTo(centerId);
        verify(paymentRecordRepository, times(1)).findByCenterId(centerId);
    }

    @Test
    void getPaymentsByCenter_ShouldThrowException_WhenCenterIdIsNull() {
        assertThatThrownBy(() -> paymentRecordService.getPaymentsByCenter(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Center ID must not be null");
    }

    @Test
    void getPaymentsByStatus_ShouldReturnPaymentRecordDTOList() {
        String status = "SUCCESS";
        when(paymentRecordRepository.findByStatus(status)).thenReturn(Collections.singletonList(sampleRecord));

        List<PaymentRecordDTO> result = paymentRecordService.getPaymentsByStatus(status);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(status);
        verify(paymentRecordRepository, times(1)).findByStatus(status);
    }

    @Test
    void getPaymentsByStatus_ShouldThrowException_WhenStatusIsEmpty() {
        assertThatThrownBy(() -> paymentRecordService.getPaymentsByStatus(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Status must not be null or empty");
    }

    @Test
    void getPaymentsByMethod_ShouldReturnPaymentRecordDTOList() {
        String method = "CARD";
        when(paymentRecordRepository.findByMethod(method)).thenReturn(Collections.singletonList(sampleRecord));

        List<PaymentRecordDTO> result = paymentRecordService.getPaymentsByMethod(method);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMethod()).isEqualTo(method);
        verify(paymentRecordRepository, times(1)).findByMethod(method);
    }

    @Test
    void getPaymentsByMethod_ShouldThrowException_WhenMethodIsEmpty() {
        assertThatThrownBy(() -> paymentRecordService.getPaymentsByMethod("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Method must not be null or empty");
    }

    @Test
    void createPayment_ShouldSaveAndReturnDTO() {
        PaymentRecordDTO dto = new PaymentRecordDTO();
        dto.setInvoiceId(invoiceId);
        dto.setCenterId(centerId);
        dto.setAmount(new BigDecimal("1550.00"));
        dto.setMethod("CARD");
        dto.setStatus("SUCCESS");

        when(paymentRecordRepository.save(any(PaymentRecord.class))).thenReturn(sampleRecord);

        PaymentRecordDTO result = paymentRecordService.createPayment(dto);

        assertThat(result).isNotNull();
        assertThat(result.getPaymentId()).isEqualTo(paymentId);
        verify(paymentRecordRepository, times(1)).save(any(PaymentRecord.class));
    }

    @Test
    void createPayment_ShouldThrowException_WhenDTOIsNull() {
        assertThatThrownBy(() -> paymentRecordService.createPayment(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment record data must not be null");
    }

    @Test
    void updatePayment_ShouldModifyAndSavePaymentRecord() {
        PaymentRecordDTO dto = new PaymentRecordDTO();
        dto.setInvoiceId(invoiceId);
        dto.setCenterId(centerId);
        dto.setAmount(new BigDecimal("2000.00"));
        dto.setMethod("CASH");
        dto.setStatus("COMPLETED");

        PaymentRecord updatedRecord = new PaymentRecord();
        updatedRecord.setPaymentId(paymentId);
        updatedRecord.setInvoiceId(invoiceId);
        updatedRecord.setCenterId(centerId);
        updatedRecord.setAmount(new BigDecimal("2000.00"));
        updatedRecord.setMethod("CASH");
        updatedRecord.setStatus("COMPLETED");

        when(paymentRecordRepository.findById(paymentId)).thenReturn(Optional.of(sampleRecord));
        when(paymentRecordRepository.save(any(PaymentRecord.class))).thenReturn(updatedRecord);

        PaymentRecordDTO result = paymentRecordService.updatePayment(paymentId, dto);

        assertThat(result).isNotNull();
        assertThat(result.getMethod()).isEqualTo("CASH");
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        verify(paymentRecordRepository, times(1)).findById(paymentId);
        verify(paymentRecordRepository, times(1)).save(any(PaymentRecord.class));
    }

    @Test
    void updatePayment_ShouldThrowException_WhenNotFound() {
        PaymentRecordDTO dto = new PaymentRecordDTO();
        when(paymentRecordRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentRecordService.updatePayment(paymentId, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Payment record not found with id");
    }

    @Test
    void deletePayment_ShouldCallRepositoryDelete_WhenPaymentRecordExists() {
        when(paymentRecordRepository.existsById(paymentId)).thenReturn(true);

        paymentRecordService.deletePayment(paymentId);

        verify(paymentRecordRepository, times(1)).existsById(paymentId);
        verify(paymentRecordRepository, times(1)).deleteById(paymentId);
    }

    @Test
    void deletePayment_ShouldThrowException_WhenPaymentRecordDoesNotExist() {
        when(paymentRecordRepository.existsById(paymentId)).thenReturn(false);

        assertThatThrownBy(() -> paymentRecordService.deletePayment(paymentId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Payment record not found with id");
    }
}
