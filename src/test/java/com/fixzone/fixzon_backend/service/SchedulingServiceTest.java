package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.enums.BookingStatus;
import com.fixzone.fixzon_backend.model.Booking;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.model.ServicePackage;
import com.fixzone.fixzon_backend.repository.BookingRepository;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.ServicePackageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SchedulingService Unit Tests")
class SchedulingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ServiceCenterRepository serviceCenterRepository;

    @Mock
    private ServicePackageRepository servicePackageRepository;

    @InjectMocks
    private SchedulingService schedulingService;

    private UUID centerId;
    private UUID packageId;
    private LocalDate testDate;
    private ServiceCenter serviceCenter;
    private ServicePackage servicePackage;

    @BeforeEach
    void setUp() {
        centerId = UUID.randomUUID();
        packageId = UUID.randomUUID();
        testDate = LocalDate.of(2026, 9, 1);

        serviceCenter = new ServiceCenter();
        serviceCenter.setCenterId(centerId);
        serviceCenter.setServiceLanesCount(1); // Default 1 lane

        servicePackage = new ServicePackage();
        servicePackage.setPackageId(packageId);
        servicePackage.setName("Oil Change");
        servicePackage.setEstimatedDurationMins(45);
    }

    @Nested
    @DisplayName("1. Operating Hours & Session Boundary Validation")
    class SessionBoundaryTests {

        @Test
        @DisplayName("Valid morning session: 08:00 AM - 10:00 AM (120 mins)")
        void validMorningSlot() {
            boolean valid = schedulingService.isValidSessionTime(LocalTime.of(8, 0), 120);
            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("Valid morning session up to exact 12:00 PM boundary (10:00 AM - 12:00 PM, 120 mins)")
        void validMorningUpTo12() {
            boolean valid = schedulingService.isValidSessionTime(LocalTime.of(10, 0), 120);
            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("Invalid: Service crosses lunch break (11:00 AM - 01:00 PM, 120 mins)")
        void crossesLunchBreak_ShouldBeInvalid() {
            boolean valid = schedulingService.isValidSessionTime(LocalTime.of(11, 0), 120);
            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("Invalid: Service starts during lunch break (12:30 PM, 45 mins)")
        void startsDuringLunch_ShouldBeInvalid() {
            boolean valid = schedulingService.isValidSessionTime(LocalTime.of(12, 30), 45);
            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("Valid evening session: 01:00 PM - 03:00 PM (120 mins)")
        void validEveningSlot() {
            boolean valid = schedulingService.isValidSessionTime(LocalTime.of(13, 0), 120);
            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("Valid evening session up to exact 06:00 PM boundary (04:00 PM - 06:00 PM, 120 mins)")
        void validEveningUpTo18() {
            boolean valid = schedulingService.isValidSessionTime(LocalTime.of(16, 0), 120);
            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("Invalid: Service exceeds evening closing time 06:00 PM (05:00 PM, 120 mins -> 07:00 PM)")
        void exceedsEveningClosing_ShouldBeInvalid() {
            boolean valid = schedulingService.isValidSessionTime(LocalTime.of(17, 0), 120);
            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("Invalid: Service starts before morning opening 08:00 AM (07:30 AM, 60 mins)")
        void startsBeforeMorning_ShouldBeInvalid() {
            boolean valid = schedulingService.isValidSessionTime(LocalTime.of(7, 30), 60);
            assertThat(valid).isFalse();
        }
    }

    @Nested
    @DisplayName("2. 15-Minute Buffer Validation")
    class BufferValidationTests {

        @Test
        @DisplayName("Same lane: Booking 08:00-09:00 blocks 09:00 start, allows 09:15 start")
        void bufferEnforcedCorrectly() {
            serviceCenter.setServiceLanesCount(1);
            when(serviceCenterRepository.findById(centerId)).thenReturn(Optional.of(serviceCenter));

            Booking existing = new Booking();
            existing.setBookingId(UUID.randomUUID());
            existing.setCenterId(centerId);
            existing.setBookingDate(testDate);
            existing.setBookingTime(LocalTime.of(8, 0));
            existing.setDurationMins(60);
            existing.setEndTime(LocalTime.of(9, 0));
            existing.setAssignedLane(1);
            existing.setStatus(BookingStatus.CONFIRMED);

            when(bookingRepository.findActiveBookingsForCenterAndDate(eq(centerId), eq(testDate), any()))
                    .thenReturn(List.of(existing));

            // Immediately at 09:00 -> NO buffer gap -> invalid
            boolean slotAt900 = schedulingService.isSlotAvailable(centerId, testDate, LocalTime.of(9, 0), 60, null);
            assertThat(slotAt900).isFalse();

            // At 09:15 -> 15 min buffer satisfied -> valid
            boolean slotAt915 = schedulingService.isSlotAvailable(centerId, testDate, LocalTime.of(9, 15), 60, null);
            assertThat(slotAt915).isTrue();
        }
    }

    @Nested
    @DisplayName("3. Multi-Lane Scheduling")
    class MultiLaneSchedulingTests {

        @Test
        @DisplayName("1-Lane Center: 08:00 is taken, second 08:00 booking is rejected")
        void singleLane_SecondBookingRejected() {
            serviceCenter.setServiceLanesCount(1);
            when(serviceCenterRepository.findById(centerId)).thenReturn(Optional.of(serviceCenter));

            Booking existing = new Booking();
            existing.setBookingTime(LocalTime.of(8, 0));
            existing.setDurationMins(60);
            existing.setEndTime(LocalTime.of(9, 0));
            existing.setAssignedLane(1);

            when(bookingRepository.findActiveBookingsForCenterAndDate(eq(centerId), eq(testDate), any()))
                    .thenReturn(List.of(existing));

            boolean available = schedulingService.isSlotAvailable(centerId, testDate, LocalTime.of(8, 0), 60, null);
            assertThat(available).isFalse();
        }

        @Test
        @DisplayName("2-Lane Center: 08:00 booked in Lane 1, second 08:00 booking in Lane 2 is accepted")
        void twoLanes_SecondBookingAccepted() {
            serviceCenter.setServiceLanesCount(2);
            when(serviceCenterRepository.findById(centerId)).thenReturn(Optional.of(serviceCenter));

            Booking existingLane1 = new Booking();
            existingLane1.setBookingTime(LocalTime.of(8, 0));
            existingLane1.setDurationMins(60);
            existingLane1.setEndTime(LocalTime.of(9, 0));
            existingLane1.setAssignedLane(1);

            when(bookingRepository.findActiveBookingsForCenterAndDate(eq(centerId), eq(testDate), any()))
                    .thenReturn(List.of(existingLane1));

            boolean available = schedulingService.isSlotAvailable(centerId, testDate, LocalTime.of(8, 0), 60, null);
            assertThat(available).isTrue();
        }

        @Test
        @DisplayName("2-Lane Center: Both lanes booked 08:00-09:00, third 08:00 booking is rejected")
        void twoLanes_BothFull_ThirdBookingRejected() {
            serviceCenter.setServiceLanesCount(2);
            when(serviceCenterRepository.findById(centerId)).thenReturn(Optional.of(serviceCenter));

            Booking existing1 = new Booking();
            existing1.setBookingTime(LocalTime.of(8, 0));
            existing1.setDurationMins(60);
            existing1.setEndTime(LocalTime.of(9, 0));
            existing1.setAssignedLane(1);

            Booking existing2 = new Booking();
            existing2.setBookingTime(LocalTime.of(8, 0));
            existing2.setDurationMins(60);
            existing2.setEndTime(LocalTime.of(9, 0));
            existing2.setAssignedLane(2);

            when(bookingRepository.findActiveBookingsForCenterAndDate(eq(centerId), eq(testDate), any()))
                    .thenReturn(List.of(existing1, existing2));

            boolean available = schedulingService.isSlotAvailable(centerId, testDate, LocalTime.of(8, 0), 60, null);
            assertThat(available).isFalse();
        }
    }

    @Nested
    @DisplayName("4. Available Start Times - Contiguous Gap-Free Scheduling")
    class AvailableStartTimesGenerationTests {

        @Test
        @DisplayName("Empty day 45-min package: Shows 08:00, 09:00, 10:00, 11:00, 11:15 without arbitrary 08:15/08:30/08:45")
        void emptyDay45MinPackage_GeneratesContiguousSlots() {
            serviceCenter.setServiceLanesCount(1);
            when(serviceCenterRepository.findById(centerId)).thenReturn(Optional.of(serviceCenter));
            when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage)); // 45 mins
            when(servicePackageRepository.findByServiceCenter_CenterIdAndIsActiveTrue(centerId))
                    .thenReturn(List.of(servicePackage));

            when(bookingRepository.findActiveBookingsForCenterAndDate(eq(centerId), eq(testDate), any()))
                    .thenReturn(Collections.emptyList());

            List<String> startTimes = schedulingService.getAvailableStartTimes(centerId, testDate, packageId);

            assertThat(startTimes).isNotEmpty();
            // Valid contiguous start times
            assertThat(startTimes).contains("08:00 AM", "09:00 AM", "10:00 AM", "11:00 AM", "11:15 AM", "01:00 PM");
            // Arbitrary fragmented 15-minute grid slots must NOT exist
            assertThat(startTimes).doesNotContain("08:15 AM", "08:30 AM", "08:45 AM", "09:15 AM", "09:30 AM", "09:45 AM");
            // Lunch break strictly excluded
            assertThat(startTimes).doesNotContain("12:00 PM", "12:15 PM", "12:30 PM", "12:45 PM");
        }

        @Test
        @DisplayName("When first booking is 08:00-08:45 (45 min): Next slot is 09:00 AM (08:45 + 15m buffer)")
        void afterFirstBooking_NextSlotIs900() {
            serviceCenter.setServiceLanesCount(1);
            when(serviceCenterRepository.findById(centerId)).thenReturn(Optional.of(serviceCenter));
            when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(servicePackage)); // 45 mins
            when(servicePackageRepository.findByServiceCenter_CenterIdAndIsActiveTrue(centerId))
                    .thenReturn(List.of(servicePackage));

            // Existing booking: 08:00 - 08:45
            Booking existing = new Booking();
            existing.setBookingTime(LocalTime.of(8, 0));
            existing.setDurationMins(45);
            existing.setEndTime(LocalTime.of(8, 45));
            existing.setAssignedLane(1);

            when(bookingRepository.findActiveBookingsForCenterAndDate(eq(centerId), eq(testDate), any()))
                    .thenReturn(List.of(existing));

            List<String> startTimes = schedulingService.getAvailableStartTimes(centerId, testDate, packageId);

            // 08:00, 08:15, 08:30, 08:45 are blocked
            assertThat(startTimes).doesNotContain("08:00 AM", "08:15 AM", "08:30 AM", "08:45 AM");
            // 09:00 AM is the first valid start time after buffer
            assertThat(startTimes).contains("09:00 AM", "10:00 AM", "11:00 AM");
        }

        @Test
        @DisplayName("60-min package (Wheel Alignment): Steps by 75m (60+15 buffer) -> 08:00, 09:15, 10:30, 11:00")
        void package60Min_StepsBy75Min() {
            ServicePackage pkg60 = new ServicePackage();
            pkg60.setPackageId(packageId);
            pkg60.setName("Wheel Alignment");
            pkg60.setEstimatedDurationMins(60);

            serviceCenter.setServiceLanesCount(1);
            when(serviceCenterRepository.findById(centerId)).thenReturn(Optional.of(serviceCenter));
            when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(pkg60));
            when(servicePackageRepository.findByServiceCenter_CenterIdAndIsActiveTrue(centerId))
                    .thenReturn(List.of(pkg60));

            when(bookingRepository.findActiveBookingsForCenterAndDate(eq(centerId), eq(testDate), any()))
                    .thenReturn(Collections.emptyList());

            List<String> startTimes = schedulingService.getAvailableStartTimes(centerId, testDate, packageId);

            assertThat(startTimes).contains("08:00 AM", "09:15 AM", "10:30 AM", "11:00 AM", "01:00 PM");
            assertThat(startTimes).doesNotContain("08:15 AM", "08:30 AM", "08:45 AM", "09:00 AM");
        }

        @Test
        @DisplayName("2hr 30min (150 min) package: Last morning slot is sharply 09:30 AM (ends at 12:00 PM)")
        void package150Min_LastMorningSlotIs930AM() {
            ServicePackage pkg150 = new ServicePackage();
            pkg150.setPackageId(packageId);
            pkg150.setName("Major Service");
            pkg150.setEstimatedDurationMins(150);

            serviceCenter.setServiceLanesCount(1);
            when(serviceCenterRepository.findById(centerId)).thenReturn(Optional.of(serviceCenter));
            when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(pkg150));
            when(servicePackageRepository.findByServiceCenter_CenterIdAndIsActiveTrue(centerId))
                    .thenReturn(List.of(pkg150));

            when(bookingRepository.findActiveBookingsForCenterAndDate(eq(centerId), eq(testDate), any()))
                    .thenReturn(Collections.emptyList());

            List<String> startTimes = schedulingService.getAvailableStartTimes(centerId, testDate, packageId);

            // Morning slots: 08:00 AM, 09:30 AM (ends at 12:00 PM)
            assertThat(startTimes).contains("08:00 AM", "09:30 AM");
            // No slots starting after 09:30 AM in the morning (since morning ends sharply at 12:00 PM)
            assertThat(startTimes).doesNotContain("09:45 AM", "10:00 AM", "10:30 AM", "11:00 AM", "11:30 AM");
            // Lunch break strictly excluded
            assertThat(startTimes).doesNotContain("12:00 PM", "12:30 PM");
            // Evening slot: 01:00 PM, 03:30 PM (ends at 06:00 PM)
            assertThat(startTimes).contains("01:00 PM", "03:30 PM");
            assertThat(startTimes).doesNotContain("04:00 PM", "04:30 PM", "05:00 PM");
        }

        @Test
        @DisplayName("1hr 30min (90 min) package: Last evening slot is sharply 04:30 PM (ends at 06:00 PM)")
        void package90Min_LastEveningSlotIs430PM() {
            ServicePackage pkg90 = new ServicePackage();
            pkg90.setPackageId(packageId);
            pkg90.setName("Brake Overhaul");
            pkg90.setEstimatedDurationMins(90);

            serviceCenter.setServiceLanesCount(1);
            when(serviceCenterRepository.findById(centerId)).thenReturn(Optional.of(serviceCenter));
            when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(pkg90));
            when(servicePackageRepository.findByServiceCenter_CenterIdAndIsActiveTrue(centerId))
                    .thenReturn(List.of(pkg90));

            when(bookingRepository.findActiveBookingsForCenterAndDate(eq(centerId), eq(testDate), any()))
                    .thenReturn(Collections.emptyList());

            List<String> startTimes = schedulingService.getAvailableStartTimes(centerId, testDate, packageId);

            // Morning slots: 08:00 AM, 09:45 AM, 10:30 AM (ends at 12:00 PM)
            assertThat(startTimes).contains("08:00 AM", "09:45 AM", "10:30 AM");
            // Evening slots: 01:00 PM, 02:45 PM, 04:30 PM (ends at 06:00 PM)
            assertThat(startTimes).contains("01:00 PM", "02:45 PM", "04:30 PM");
            // No slots starting after 04:30 PM (since evening ends sharply at 06:00 PM)
            assertThat(startTimes).doesNotContain("04:45 PM", "05:00 PM", "05:15 PM", "05:30 PM");
        }
    }
}
