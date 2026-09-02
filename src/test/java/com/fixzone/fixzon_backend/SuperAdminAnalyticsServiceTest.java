package com.fixzone.fixzon_backend;

import com.fixzone.fixzon_backend.DTO.SuperAdminAnalyticsDTO;
import com.fixzone.fixzon_backend.repository.InvoiceRepository;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.SubscriptionBillingRepository;
import com.fixzone.fixzon_backend.repository.SubscriptionRepository;
import com.fixzone.fixzon_backend.service.SuperAdminAnalyticsService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * =========================================================
 *  Unit Tests – SuperAdminAnalyticsService
 *  Section: 7.2.x  Super Admin Analytics Dashboard
 * =========================================================
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SuperAdminAnalyticsService – Unit Tests")
class SuperAdminAnalyticsServiceTest {

    @Mock private SubscriptionBillingRepository subscriptionBillingRepository;
    @Mock private SubscriptionRepository        subscriptionRepository;
    @Mock private ServiceCenterRepository       serviceCenterRepository;
    @Mock private InvoiceRepository             invoiceRepository;

    @InjectMocks
    private SuperAdminAnalyticsService analyticsService;

    // ─────────────────────────────────────────────────────────────
    //  SECTION 7 – Analytics Platform Revenue
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("7. Platform Revenue Analytics")
    class PlatformRevenueTests {

        @BeforeEach
        void setUpCommonMocks() {
            // subscription_billing — safe defaults for revenue queries
            lenient().when(subscriptionBillingRepository.findMonthlyRevenueSince(any(LocalDateTime.class)))
                    .thenReturn(List.of());
            lenient().when(subscriptionBillingRepository.findDailyRevenueBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(List.of());

            // service centers
            lenient().when(serviceCenterRepository.count()).thenReturn(0L);
            lenient().when(serviceCenterRepository.findByStatus("PENDING")).thenReturn(List.of());

            // subscriptions — stat card counts
            lenient().when(subscriptionRepository.countByStatus("ACTIVE")).thenReturn(0L);
            lenient().when(subscriptionRepository.countByStartDateAfter(any(LocalDate.class))).thenReturn(0L);
            lenient().when(subscriptionRepository.countByStartDateBetween(any(), any())).thenReturn(0L);

            // subscriptions — trend chart counts
            lenient().when(subscriptionRepository.countNewSubscribersPerDayOfWeek(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of());
            lenient().when(subscriptionRepository.countNewSubscribersPerMonth(any(LocalDate.class)))
                    .thenReturn(List.of());

            // invoices — top stations only
            lenient().when(invoiceRepository.findTopCentersByRevenue(any(PageRequest.class))).thenReturn(List.of());
        }

        @Test
        @DisplayName("TC-BE-31: getAnalytics returns non-null DTO")
        void getAnalytics_returnsNonNullDTO() {
            SuperAdminAnalyticsDTO dto = analyticsService.getAnalytics();
            assertThat(dto).isNotNull();
        }

        @Test
        @DisplayName("TC-BE-32: getAnalytics sets totalPlatformRevenue from subscription_billing sum")
        void getAnalytics_setsTotalRevenue() {
            // Simulate two monthly billing rows: Jan=300000, Feb=200000 → total=500000
            Object[] row1 = {2024.0, 1.0, "300000"};
            Object[] row2 = {2024.0, 2.0, "200000"};
            when(subscriptionBillingRepository.findMonthlyRevenueSince(any(LocalDateTime.class)))
                    .thenReturn(List.of(row1, row2));

            SuperAdminAnalyticsDTO dto = analyticsService.getAnalytics();

            assertThat(dto.getTotalPlatformRevenue()).isEqualByComparingTo(new BigDecimal("500000"));
        }

        @Test
        @DisplayName("TC-BE-33: getAnalytics defaults totalPlatformRevenue to ZERO when no billing records")
        void getAnalytics_defaultsRevenueToZeroWhenEmpty() {
            when(subscriptionBillingRepository.findMonthlyRevenueSince(any(LocalDateTime.class)))
                    .thenReturn(List.of());

            SuperAdminAnalyticsDTO dto = analyticsService.getAnalytics();

            assertThat(dto.getTotalPlatformRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("TC-BE-34: getAnalytics sets totalServiceCenters from repository count")
        void getAnalytics_setsTotalServiceCenters() {
            when(serviceCenterRepository.count()).thenReturn(15L);

            SuperAdminAnalyticsDTO dto = analyticsService.getAnalytics();

            assertThat(dto.getTotalServiceCenters()).isEqualTo(15L);
        }

        @Test
        @DisplayName("TC-BE-35: getAnalytics sets activeSubscriptions count correctly")
        void getAnalytics_setsActiveSubscriptions() {
            when(subscriptionRepository.countByStatus("ACTIVE")).thenReturn(8L);

            SuperAdminAnalyticsDTO dto = analyticsService.getAnalytics();

            assertThat(dto.getActiveSubscriptions()).isEqualTo(8L);
        }

        @Test
        @DisplayName("TC-BE-36: getAnalytics weeklyRevenue list has 7 entries (Sun–Sat)")
        void getAnalytics_weeklyRevenueHasSevenEntries() {
            SuperAdminAnalyticsDTO dto = analyticsService.getAnalytics();

            assertThat(dto.getWeeklyRevenue()).hasSize(7);
        }

        @Test
        @DisplayName("TC-BE-37: getAnalytics monthlyRevenue list has 6 entries (last 6 months)")
        void getAnalytics_monthlyRevenueHasSixEntries() {
            SuperAdminAnalyticsDTO dto = analyticsService.getAnalytics();

            assertThat(dto.getMonthlyRevenue()).hasSize(6);
        }

        @Test
        @DisplayName("TC-BE-36b: getAnalytics weeklySubscribers list has 7 entries (Sun–Sat)")
        void getAnalytics_weeklySubscribersHasSevenEntries() {
            SuperAdminAnalyticsDTO dto = analyticsService.getAnalytics();

            assertThat(dto.getWeeklySubscribers()).hasSize(7);
        }

        @Test
        @DisplayName("TC-BE-37b: getAnalytics monthlySubscribers list has 6 entries (last 6 months)")
        void getAnalytics_monthlySubscribersHasSixEntries() {
            SuperAdminAnalyticsDTO dto = analyticsService.getAnalytics();

            assertThat(dto.getMonthlySubscribers()).hasSize(6);
        }

        @Test
        @DisplayName("TC-BE-36c: weeklySubscribers count reflects actual subscriber data")
        void getAnalytics_weeklySubscribersCount_reflectsData() {
            // Simulate 3 new subscribers on Monday (DOW=1)
            List<Object[]> weeklySubRows = new ArrayList<>();
            weeklySubRows.add(new Object[]{1.0, 3L});
            when(subscriptionRepository.countNewSubscribersPerDayOfWeek(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(weeklySubRows);

            SuperAdminAnalyticsDTO dto = analyticsService.getAnalytics();

            long mondayCount = dto.getWeeklySubscribers().stream()
                    .filter(t -> "Mon".equals(t.getLabel()))
                    .mapToLong(SuperAdminAnalyticsDTO.SubscriberTrendDTO::getCount)
                    .sum();
            assertThat(mondayCount).isEqualTo(3L);
        }

        @Test
        @DisplayName("TC-BE-37c: weeklyRevenue reflects billing data — not invoice data")
        void getAnalytics_weeklyRevenue_usesBillingNotInvoices() {
            // Simulate billing on Wednesday (DOW=3) = 50000
            List<Object[]> billingRows = new ArrayList<>();
            billingRows.add(new Object[]{3.0, "50000"});
            when(subscriptionBillingRepository.findDailyRevenueBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(billingRows);

            SuperAdminAnalyticsDTO dto = analyticsService.getAnalytics();

            BigDecimal wednesdayAmount = dto.getWeeklyRevenue().stream()
                    .filter(r -> "Wed".equals(r.getLabel()))
                    .map(SuperAdminAnalyticsDTO.RevenueBarDTO::getAmount)
                    .findFirst().orElse(BigDecimal.ZERO);
            assertThat(wednesdayAmount).isEqualByComparingTo(new BigDecimal("50000"));
        }

        @Test
        @DisplayName("TC-BE-38: getAnalytics topStations is empty when no data")
        void getAnalytics_topStationsIsEmpty_whenNoData() {
            SuperAdminAnalyticsDTO dto = analyticsService.getAnalytics();

            assertThat(dto.getTopStations()).isEmpty();
        }

        @Test
        @DisplayName("TC-BE-39: revenueChange is +100% when previous period is zero and current > 0")
        void getAnalytics_revenueChange_isPlus100WhenPreviousZero() {
            // current period (daily query) returns 50000, previous period returns 0
            List<Object[]> currentRows = new ArrayList<>();
            currentRows.add(new Object[]{1.0, "50000"});
            when(subscriptionBillingRepository.findDailyRevenueBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(currentRows)   // first call = current 30 days
                    .thenReturn(new ArrayList<>());  // second call = previous 30 days (0)

            SuperAdminAnalyticsDTO dto = analyticsService.getAnalytics();

            assertThat(dto.getRevenueChange()).isEqualTo("+100%");
        }

        @Test
        @DisplayName("TC-BE-40: pendingRegistrations count equals PENDING centers list size")
        void getAnalytics_pendingRegistrations_matchesPendingList() {
            com.fixzone.fixzon_backend.model.ServiceCenter p1 = new com.fixzone.fixzon_backend.model.ServiceCenter();
            com.fixzone.fixzon_backend.model.ServiceCenter p2 = new com.fixzone.fixzon_backend.model.ServiceCenter();
            when(serviceCenterRepository.findByStatus("PENDING")).thenReturn(List.of(p1, p2));

            SuperAdminAnalyticsDTO dto = analyticsService.getAnalytics();

            assertThat(dto.getPendingRegistrations()).isEqualTo(2L);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  SECTION 8 – SuperAdminService CRUD
    // ─────────────────────────────────────────────────────────────
}
