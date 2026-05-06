package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.enums.BookingStatus;
import com.fixzone.fixzon_backend.model.Booking;
import com.fixzone.fixzon_backend.repository.BookingRepository;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.ServicePackageRepository;
import com.fixzone.fixzon_backend.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ServiceCenterRepository serviceCenterRepository;

    @Mock
    private ServicePackageRepository servicePackageRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private BookingService bookingService;

    private UUID bookingId;
    private Booking inProgressBooking;

    @BeforeEach
    void setUp() {
        bookingId = UUID.randomUUID();
        inProgressBooking = new Booking();
        inProgressBooking.setBookingId(bookingId);
        inProgressBooking.setStatus(BookingStatus.IN_PROGRESS);
        inProgressBooking.setBookingDate(LocalDate.now().plusDays(5)); // More than 3 days
    }

    @Test
    void testRescheduleFailsForInProgressBooking() {
        // Arrange
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(inProgressBooking));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            bookingService.rescheduleBooking(bookingId, LocalDate.now().plusDays(10), LocalTime.of(10, 0));
        });

        assertEquals("Cannot reschedule a cancelled, completed, or in-progress booking", exception.getMessage());
    }
}
