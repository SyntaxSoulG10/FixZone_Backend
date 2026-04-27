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
        LocalDateTime now = LocalDateTime.now();

        // 1. Stat Cards
        try {
            BigDecimal totalRevenue = invoiceRepository.sumTotalRevenue();
            dto.setTotalPlatformRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
            
            // Revenue Change (30 days vs previous 30 days)
            LocalDateTime thirtyDaysAgo = now.minusDays(30);
            LocalDateTime sixtyDaysAgo = now.minusDays(60);
            
            BigDecimal currentPeriodRevenue = invoiceRepository.sumRevenueBetween(thirtyDaysAgo, now);
            BigDecimal previousPeriodRevenue = invoiceRepository.sumRevenueBetween(sixtyDaysAgo, thirtyDaysAgo);
            dto.setRevenueChange(calculatePercentageChange(currentPeriodRevenue, previousPeriodRevenue));

            dto.setTotalServiceCenters(serviceCenterRepository.count());
            dto.setPendingRegistrations((long) serviceCenterRepository.findByStatus("PENDING").size());
            dto.setActiveSubscriptions(subscriptionRepository.countByStatus("ACTIVE"));
            
            // Subscription Change (30 days vs previous 30 days) - using count as a proxy for growth
            // Note: Since Subscription model doesn't have createdAt, we use startDate as a proxy
            long currentSubs = subscriptionRepository.countByStartDateAfter(thirtyDaysAgo.toLocalDate());
            long previousSubs = subscriptionRepository.countByStartDateBetween(sixtyDaysAgo.toLocalDate(), thirtyDaysAgo.toLocalDate());
            dto.setSubscriptionChange(calculateGrowth(currentSubs, previousSubs));
        } catch (Exception e) {
            System.err.println("Error calculating stat cards: " + e.getMessage());
            dto.setTotalPlatformRevenue(BigDecimal.ZERO);
            dto.setRevenueChange("0%");
            dto.setSubscriptionChange("0%");
        }

        // 2. Weekly Revenue (Last 7 days)
        try {
            LocalDateTime startOfWeek = now.minusDays(6).withHour(0).withMinute(0).withSecond(0);
            List<Object[]> dailyData = invoiceRepository.findDailyRevenueBetween(startOfWeek, now);
            dto.setWeeklyRevenue(formatWeeklyRevenue(dailyData));
        } catch (Exception e) {
            System.err.println("Error calculating weekly revenue: " + e.getMessage());
            dto.setWeeklyRevenue(new ArrayList<>());
        }

        // 3. Monthly Revenue (Last 6 months)
        try {
            LocalDateTime sixMonthsAgo = now.minusMonths(5).withDayOfMonth(1).withHour(0).withMinute(0);
            List<Object[]> monthlyData = invoiceRepository.findMonthlyRevenueSince(sixMonthsAgo);
            dto.setMonthlyRevenue(formatMonthlyRevenue(monthlyData));
        } catch (Exception e) {
            System.err.println("Error calculating monthly revenue: " + e.getMessage());
            dto.setMonthlyRevenue(new ArrayList<>());
        }

        // 4. Top Stations
        try {
            List<Object[]> topCentersData = invoiceRepository.findTopCentersByRevenue(PageRequest.of(0, 5));
            dto.setTopStations(formatTopStations(topCentersData));
        } catch (Exception e) {
            System.err.println("Error calculating top stations: " + e.getMessage());
            dto.setTopStations(new ArrayList<>());
        }

        return dto;
    }

    private String calculatePercentageChange(BigDecimal current, BigDecimal previous) {
        if (current == null) current = BigDecimal.ZERO;
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? "+100%" : "0%";
        }
        
        try {
            BigDecimal change = current.subtract(previous)
                    .divide(previous, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100))
                    .setScale(1, RoundingMode.HALF_UP);
            
            return (change.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + change + "%";
        } catch (Exception e) {
            return "0%";
        }
    }

    private String calculateGrowth(long current, long previous) {
        if (current == 0 && previous == 0) return "0%";
        if (previous == 0) return "+" + current + " new";
        
        double change = ((double)(current - previous) / previous) * 100;
        return (change >= 0 ? "+" : "") + String.format("%.1f", change) + "%";
    }

    private List<SuperAdminAnalyticsDTO.RevenueBarDTO> formatWeeklyRevenue(List<Object[]> data) {
        Map<Integer, BigDecimal> dataMap = new HashMap<>();
        if (data != null) {
            for (Object[] row : data) {
                if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                    try {
                        int dow = ((Number) row[0]).intValue();
                        dataMap.put(dow, new BigDecimal(row[1].toString()));
                    } catch (Exception e) {
                        System.err.println("Error parsing weekly revenue row: " + e.getMessage());
                    }
                }
            }
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
            int percentage = amount.multiply(new BigDecimal(100))
                    .divide(max, 0, RoundingMode.HALF_UP).intValue();
            result.add(new SuperAdminAnalyticsDTO.RevenueBarDTO(days[i], amount, percentage));
        }
        return result;
    }

    private List<SuperAdminAnalyticsDTO.RevenueBarDTO> formatMonthlyRevenue(List<Object[]> data) {
        Map<String, BigDecimal> dataMap = new HashMap<>();
        if (data != null) {
            for (Object[] row : data) {
                if (row != null && row.length >= 3 && row[1] != null && row[2] != null) {
                    try {
                        int monthVal = ((Number) row[1]).intValue();
                        // PostgreSQL EXTRACT(MONTH) returns 1-12
                        if (monthVal >= 1 && monthVal <= 12) {
                            String label = Month.of(monthVal).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                            dataMap.put(label, new BigDecimal(row[2].toString()));
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing monthly revenue row: " + e.getMessage());
                    }
                }
            }
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
            int percentage = amount.multiply(new BigDecimal(100))
                    .divide(max, 0, RoundingMode.HALF_UP).intValue();
            result.add(new SuperAdminAnalyticsDTO.RevenueBarDTO(label, amount, percentage));
        }
        return result;
    }

    private List<SuperAdminAnalyticsDTO.TopStationDTO> formatTopStations(List<Object[]> data) {
        if (data == null) return new ArrayList<>();
        return data.stream().map(row -> {
            try {
                Object centerIdObj = row[0];
                if (centerIdObj == null) return new SuperAdminAnalyticsDTO.TopStationDTO("Unknown", BigDecimal.ZERO, "Rs 0");
                
                UUID centerId;
                if (centerIdObj instanceof UUID) {
                    centerId = (UUID) centerIdObj;
                } else if (centerIdObj instanceof byte[]) {
                    centerId = convertBytesToUUID((byte[]) centerIdObj);
                } else {
                    centerId = UUID.fromString(centerIdObj.toString());
                }
                
                BigDecimal revenue = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
                
                String name = serviceCenterRepository.findById(centerId)
                        .map(ServiceCenter::getName)
                        .orElse("Unknown Station");
                
                String formattedRevenue = "Rs " + formatCurrency(revenue);
                return new SuperAdminAnalyticsDTO.TopStationDTO(name, revenue, formattedRevenue);
            } catch (Exception e) {
                System.err.println("Error parsing top station row: " + e.getMessage());
                return new SuperAdminAnalyticsDTO.TopStationDTO("Unknown", BigDecimal.ZERO, "Rs 0");
            }
        }).collect(Collectors.toList());
    }

    private UUID convertBytesToUUID(byte[] bytes) {
        if (bytes.length != 16) return null;
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(bytes);
        return new UUID(bb.getLong(), bb.getLong());
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
