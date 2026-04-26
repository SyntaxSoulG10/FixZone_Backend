package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.SuperAdminAnalyticsDTO;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.repository.InvoiceRepository;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.SubscriptionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SuperAdminAnalyticsService {

    private final InvoiceRepository invoiceRepository;
    private final ServiceCenterRepository serviceCenterRepository;
    private final SubscriptionRepository subscriptionRepository;

    public SuperAdminAnalyticsService(InvoiceRepository invoiceRepository,
                                     ServiceCenterRepository serviceCenterRepository,
                                     SubscriptionRepository subscriptionRepository) {
        this.invoiceRepository = invoiceRepository;
        this.serviceCenterRepository = serviceCenterRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    public SuperAdminAnalyticsDTO getAnalytics() {
        SuperAdminAnalyticsDTO dto = new SuperAdminAnalyticsDTO();

        // 1. Stat Cards
        BigDecimal totalRevenue = invoiceRepository.sumTotalRevenue();
        dto.setTotalPlatformRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        
        // Revenue Change (30 days vs previous 30 days)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minusDays(30);
        LocalDateTime sixtyDaysAgo = now.minusDays(60);
        
        BigDecimal currentPeriodRevenue = invoiceRepository.sumRevenueBetween(thirtyDaysAgo, now);
        BigDecimal previousPeriodRevenue = invoiceRepository.sumRevenueBetween(sixtyDaysAgo, thirtyDaysAgo);
        dto.setRevenueChange(calculatePercentageChange(currentPeriodRevenue, previousPeriodRevenue));

        dto.setTotalServiceCenters(serviceCenterRepository.count());
        dto.setPendingRegistrations((long) serviceCenterRepository.findByStatus("PENDING").size());
        dto.setActiveSubscriptions(subscriptionRepository.countByStatus("ACTIVE"));
        
        // Subscription Change (Simplified placeholder for now)
        dto.setSubscriptionChange("+5.7%"); 

        // 2. Weekly Revenue (Last 7 days)
        LocalDateTime startOfWeek = now.minusDays(6).withHour(0).withMinute(0).withSecond(0);
        List<Object[]> dailyData = invoiceRepository.findDailyRevenueBetween(startOfWeek, now);
        dto.setWeeklyRevenue(formatWeeklyRevenue(dailyData));

        // 3. Monthly Revenue (Last 6 months)
        LocalDateTime sixMonthsAgo = now.minusMonths(5).withDayOfMonth(1).withHour(0).withMinute(0);
        List<Object[]> monthlyData = invoiceRepository.findMonthlyRevenueSince(sixMonthsAgo);
        dto.setMonthlyRevenue(formatMonthlyRevenue(monthlyData));

        // 4. Top Stations
        List<Object[]> topCentersData = invoiceRepository.findTopCentersByRevenue(PageRequest.of(0, 5));
        dto.setTopStations(formatTopStations(topCentersData));

        return dto;
    }

    private String calculatePercentageChange(BigDecimal current, BigDecimal previous) {
        if (current == null) current = BigDecimal.ZERO;
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? "+100%" : "0%";
        }
        
        BigDecimal change = current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100))
                .setScale(1, RoundingMode.HALF_UP);
        
        return (change.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + change + "%";
    }

    private List<SuperAdminAnalyticsDTO.RevenueBarDTO> formatWeeklyRevenue(List<Object[]> data) {
        Map<Integer, BigDecimal> dataMap = new HashMap<>();
        for (Object[] row : data) {
            // PostgreSQL EXTRACT(DOW) usually returns Double
            int dow = ((Number) row[0]).intValue();
            dataMap.put(dow, (BigDecimal) row[1]);
        }

        List<SuperAdminAnalyticsDTO.RevenueBarDTO> result = new ArrayList<>();
        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        
        BigDecimal max = dataMap.values().stream()
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ONE);
        if (max.compareTo(BigDecimal.ZERO) == 0) max = BigDecimal.ONE;

        for (int i = 0; i < 7; i++) {
            BigDecimal amount = dataMap.getOrDefault(i, BigDecimal.ZERO);
            if (amount == null) amount = BigDecimal.ZERO;
            int percentage = amount.multiply(new BigDecimal(100))
                    .divide(max, 0, RoundingMode.HALF_UP).intValue();
            result.add(new SuperAdminAnalyticsDTO.RevenueBarDTO(days[i], amount, percentage));
        }
        return result;
    }

    private List<SuperAdminAnalyticsDTO.RevenueBarDTO> formatMonthlyRevenue(List<Object[]> data) {
        Map<String, BigDecimal> dataMap = new HashMap<>();
        for (Object[] row : data) {
            int monthVal = ((Number) row[1]).intValue();
            String label = Month.of(monthVal).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            dataMap.put(label, (BigDecimal) row[2]);
        }

        BigDecimal max = dataMap.values().stream()
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ONE);
        if (max.compareTo(BigDecimal.ZERO) == 0) max = BigDecimal.ONE;

        List<SuperAdminAnalyticsDTO.RevenueBarDTO> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 5; i >= 0; i--) {
            String label = now.minusMonths(i).getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            BigDecimal amount = dataMap.getOrDefault(label, BigDecimal.ZERO);
            if (amount == null) amount = BigDecimal.ZERO;
            int percentage = amount.multiply(new BigDecimal(100))
                    .divide(max, 0, RoundingMode.HALF_UP).intValue();
            result.add(new SuperAdminAnalyticsDTO.RevenueBarDTO(label, amount, percentage));
        }
        return result;
    }

    private List<SuperAdminAnalyticsDTO.TopStationDTO> formatTopStations(List<Object[]> data) {
        return data.stream().map(row -> {
            UUID centerId = (UUID) row[0];
            BigDecimal revenue = (BigDecimal) row[1];
            String name = serviceCenterRepository.findById(centerId)
                    .map(ServiceCenter::getName)
                    .orElse("Unknown Station");
            
            String formattedRevenue = "Rs " + formatCurrency(revenue);
            return new SuperAdminAnalyticsDTO.TopStationDTO(name, revenue, formattedRevenue);
        }).collect(Collectors.toList());
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0";
        if (amount.compareTo(new BigDecimal(1000000)) >= 0) {
            return amount.divide(new BigDecimal(1000000), 1, RoundingMode.HALF_UP) + "M";
        } else if (amount.compareTo(new BigDecimal(1000)) >= 0) {
            return amount.divide(new BigDecimal(1000), 1, RoundingMode.HALF_UP) + "K";
        }
        return amount.setScale(0, RoundingMode.HALF_UP).toString();
    }
}
