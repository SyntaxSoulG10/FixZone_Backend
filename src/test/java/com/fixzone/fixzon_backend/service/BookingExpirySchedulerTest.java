package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.enums.BookingStatus;
import com.fixzone.fixzon_backend.model.Booking;
import com.fixzone.fixzon_backend.repository.BookingRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingExpiryScheduler Unit Tests")
class BookingExpirySchedulerTest {

    @Mock BookingRepository bookingRepository;
    @InjectMocks BookingExpiryScheduler scheduler;

    Booking expiredBooking;

    @BeforeEach
    void setUp() {
        expiredBooking = new Booking();
        expiredBooking.setBookingId(UUID.randomUUID());
        expiredBooking.setStatus(BookingStatus.PENDING_PAYMENT);
        expiredBooking.setExpiresAt(LocalDateTime.now().minusMinutes(1)); // already past
    }

    @Test
    @DisplayName("Expired PENDING_PAYMENT booking transitions to EXPIRED")
    void expiredBookingSetToExpired() {
        when(bookingRepository.findExpiredBookings(any())).thenReturn(List.of(expiredBooking));
        when(bookingRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.expireStaleBookings();

        assertThat(expiredBooking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        verify(bookingRepository).saveAll(List.of(expiredBooking));
    }

    @Test
    @DisplayName("saveAll called with all expired bookings in the list")
    void saveAllCalledWithFullList() {
        Booking second = new Booking();
        second.setBookingId(UUID.randomUUID());
        second.setStatus(BookingStatus.PENDING_PAYMENT);
        second.setExpiresAt(LocalDateTime.now().minusSeconds(30));

        when(bookingRepository.findExpiredBookings(any())).thenReturn(List.of(expiredBooking, second));
        when(bookingRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.expireStaleBookings();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Booking>> captor = ArgumentCaptor.forClass(List.class);
        verify(bookingRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue()).allMatch(b -> b.getStatus() == BookingStatus.EXPIRED);
    }

    @Test
    @DisplayName("No saveAll call when no bookings are expired")
    void noSaveWhenNothingExpired() {
        when(bookingRepository.findExpiredBookings(any())).thenReturn(Collections.emptyList());

        scheduler.expireStaleBookings();

        verify(bookingRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Scheduler passes current time to the repository query")
    void currentTimePassedToRepository() {
        when(bookingRepository.findExpiredBookings(any())).thenReturn(Collections.emptyList());

        LocalDateTime before = LocalDateTime.now();
        scheduler.expireStaleBookings();
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(bookingRepository).findExpiredBookings(captor.capture());

        assertThat(captor.getValue()).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    }
}
