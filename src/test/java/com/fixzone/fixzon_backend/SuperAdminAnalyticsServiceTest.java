package com.fixzone.fixzon_backend;

import com.fixzone.fixzon_backend.DTO.SuperAdminAnalyticsDTO;
import com.fixzone.fixzon_backend.repository.InvoiceRepository;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.SubscriptionRepository;
import com.fixzone.fixzon_backend.service.SuperAdminAnalyticsService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Mock private InvoiceRepository       invoiceRepository;
    @Mock private ServiceCenterRepository serviceCenterRepository;
    @Mock private SubscriptionRepository  subscriptionRepository;

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
            // Safe defaults so every test doesn't need to stub all calls
            when(invoiceRepository.sumTotalRevenue()).thenReturn(BigDecimal.ZERO);
            when(invoiceRepository.sumRevenueBetween(any(), any())).thenReturn(BigDecimal.ZERO);
            when(serviceCenterRepository.count()).thenReturn(0L);
            when(serviceCenterRepository.findByStatus("PENDING")).thenReturn(List.of());
            when(subscriptionRepository.countByStatus("ACTIVE")).thenReturn(0L);
            when(subscriptionRepository.countByStartDateAfter(any(LocalDate.class))).thenReturn(0L);
            when(subscriptionRepository.countByStartDateBetween(any(), any())).thenReturn(0L);
            when(invoiceRepository.findDailyRevenueBetween(any(), any())).thenReturn(List.of());
            when(invoiceRepository.findMonthlyRevenueSince(any())).thenReturn(List.of());
            when(invoiceRepository.findTopCentersByRevenue(any(PageRequest.class))).thenReturn(List.of());
        }

        @Test
        @DisplayName("TC-BE-31: getAnalytics returns non-null DTO")
        void getAnalytics_returnsNonNullDTO() {
            SuperAdminAnalyticsDTO dto = analyticsService.getAnalytics();
            assertThat(dto).isNotNull();
        }

        @Test
        @DisplayName("TC-BE-32: getAnalytics sets totalPlatformRevenue from invoice sum")
        void getAnalytics_setsTotalRevenue() {
            when(invoiceRepository.sumTotalRevenue()).thenReturn(new BigDecimal("500000"));

            SuperAdminAnalyticsDTO dto = analyticsService.getAnalytics();

            assertThat(dto.getTotalPlatformRevenue()).isEqualByComparingTo(new BigDecimal("500000"));
        }

        @Test
        @DisplayName("TC-BE-33: getAnalytics defaults totalPlatformRevenue to ZERO when null returned")
        void getAnalytics_defaultsRevenueToZeroWhenNull() {
            when(invoiceRepository.sumTotalRevenue()).thenReturn(null);

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
        @DisplayName("TC-BE-36: getAnalytics weeklyRevenue list has 7 entries")
        void getAnalytics_weeklyRevenueHasSevenEntries() {
            SuperAdminAnalyticsDTO dto = analyticsService.getAnalytics();

            assertThat(dto.getWeeklyRevenue()).hasSize(7);
        }

        @Test
        @DisplayName("TC-BE-37: getAnalytics monthlyRevenue list has 6 entries")
        void getAnalytics_monthlyRevenueHasSixEntries() {
            SuperAdminAnalyticsDTO dto = analyticsService.getAnalytics();

            assertThat(dto.getMonthlyRevenue()).hasSize(6);
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
            when(invoiceRepository.sumTotalRevenue()).thenReturn(new BigDecimal("100000"));
            when(invoiceRepository.sumRevenueBetween(any(), any()))
                    .thenReturn(new BigDecimal("50000"))   // current 30 days
                    .thenReturn(BigDecimal.ZERO);           // previous 30 days

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
