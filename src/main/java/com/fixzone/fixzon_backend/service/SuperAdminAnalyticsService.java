package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.SuperAdminAnalyticsDTO;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.repository.InvoiceRepository;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.SubscriptionBillingRepository;
import com.fixzone.fixzon_backend.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SuperAdminAnalyticsService {
    private static final Logger log = LoggerFactory.getLogger(SuperAdminAnalyticsService.class);

    private final SubscriptionBillingRepository subscriptionBillingRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ServiceCenterRepository serviceCenterRepository;
    private final InvoiceRepository invoiceRepository;

    public SuperAdminAnalyticsService(SubscriptionBillingRepository subscriptionBillingRepository,
                                      SubscriptionRepository subscriptionRepository,
                                      ServiceCenterRepository serviceCenterRepository,
                                      InvoiceRepository invoiceRepository) {
        this.subscriptionBillingRepository = subscriptionBillingRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.serviceCenterRepository = serviceCenterRepository;
        this.invoiceRepository = invoiceRepository;
    }

    public SuperAdminAnalyticsDTO getAnalytics() {
        SuperAdminAnalyticsDTO dto = new SuperAdminAnalyticsDTO();
        LocalDateTime now = LocalDateTime.now();

        // ─── 1. Stat Cards ────────────────────────────────────────────────────────
        try {
            // Total platform revenue = sum of ALL subscription billing payments
            List<Object[]> allBilling = subscriptionBillingRepository.findMonthlyRevenueSince(LocalDateTime.of(2000, 1, 1, 0, 0));
            BigDecimal totalRevenue = allBilling.stream()
                    .map(row -> row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setTotalPlatformRevenue(totalRevenue);

            // Revenue change: last 30 days vs previous 30 days (from subscription_billing)
            LocalDateTime thirtyDaysAgo = now.minusDays(30);
            LocalDateTime sixtyDaysAgo = now.minusDays(60);
            BigDecimal currentRevenue = sumBillingBetween(thirtyDaysAgo, now);
            BigDecimal previousRevenue = sumBillingBetween(sixtyDaysAgo, thirtyDaysAgo);
            dto.setRevenueChange(calculatePercentageChange(currentRevenue, previousRevenue));

            dto.setTotalServiceCenters(serviceCenterRepository.count());
            dto.setPendingRegistrations((long) serviceCenterRepository.findByStatus("PENDING").size());
            dto.setActiveSubscriptions(subscriptionRepository.countByStatus("ACTIVE"));

            // Subscription growth: new subscribers this 30 days vs previous 30 days
            long currentSubs = subscriptionRepository.countByStartDateAfter(thirtyDaysAgo.toLocalDate());
            long previousSubs = subscriptionRepository.countByStartDateBetween(sixtyDaysAgo.toLocalDate(), thirtyDaysAgo.toLocalDate());
            dto.setSubscriptionChange(calculateGrowth(currentSubs, previousSubs));
        } catch (Exception e) {
            log.error("Error calculating stat cards: {}", e.getMessage(), e);
            dto.setTotalPlatformRevenue(BigDecimal.ZERO);
            dto.setRevenueChange("0%");
            dto.setSubscriptionChange("0%");
        }

        // ─── 2. Weekly Revenue Chart (subscription_billing, last 7 days) ──────────
        try {
            LocalDateTime startOfWeek = now.minusDays(6).withHour(0).withMinute(0).withSecond(0);
            List<Object[]> dailyData = subscriptionBillingRepository.findDailyRevenueBetween(startOfWeek, now);
            dto.setWeeklyRevenue(formatWeeklyRevenue(dailyData));
        } catch (Exception e) {
            log.error("Error calculating weekly revenue: {}", e.getMessage(), e);
            dto.setWeeklyRevenue(new ArrayList<>());
        }

        // ─── 3. Monthly Revenue Chart (subscription_billing, last 6 months) ───────
        try {
            LocalDateTime sixMonthsAgo = now.minusMonths(5).withDayOfMonth(1).withHour(0).withMinute(0);
            List<Object[]> monthlyData = subscriptionBillingRepository.findMonthlyRevenueSince(sixMonthsAgo);
            dto.setMonthlyRevenue(formatMonthlyRevenue(monthlyData));
        } catch (Exception e) {
            log.error("Error calculating monthly revenue: {}", e.getMessage(), e);
            dto.setMonthlyRevenue(new ArrayList<>());
        }

        // ─── 4. Weekly Subscriber Trend (subscriptions.start_date, last 7 days) ───
        try {
            LocalDate startOfWeek = now.minusDays(6).toLocalDate();
            LocalDate today = now.toLocalDate();
            List<Object[]> weeklySubData = subscriptionRepository.countNewSubscribersPerDayOfWeek(startOfWeek, today);
            dto.setWeeklySubscribers(formatWeeklySubscribers(weeklySubData));
        } catch (Exception e) {
            log.error("Error calculating weekly subscriber trend: {}", e.getMessage(), e);
            dto.setWeeklySubscribers(new ArrayList<>());
        }

        // ─── 5. Monthly Subscriber Trend (subscriptions.start_date, last 6 months) ─
        try {
            LocalDate sixMonthsAgo = now.minusMonths(5).withDayOfMonth(1).toLocalDate();
            List<Object[]> monthlySubData = subscriptionRepository.countNewSubscribersPerMonth(sixMonthsAgo);
            dto.setMonthlySubscribers(formatMonthlySubscribers(monthlySubData));
        } catch (Exception e) {
            log.error("Error calculating monthly subscriber trend: {}", e.getMessage(), e);
            dto.setMonthlySubscribers(new ArrayList<>());
        }

        // ─── 6. Top Stations (still from invoices — service-level revenue) ─────────
        try {
            List<Object[]> topCentersData = invoiceRepository.findTopCentersByRevenue(PageRequest.of(0, 5));
            dto.setTopStations(formatTopStations(topCentersData));
        } catch (Exception e) {
            log.error("Error calculating top stations: {}", e.getMessage(), e);
            dto.setTopStations(new ArrayList<>());
        }

        return dto;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────────

    private BigDecimal sumBillingBetween(LocalDateTime start, LocalDateTime end) {
        try {
            List<Object[]> data = subscriptionBillingRepository.findDailyRevenueBetween(start, end);
            return data.stream()
                    .map(row -> row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
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
                        log.warn("Error parsing weekly revenue row: {}", e.getMessage());
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
                        if (monthVal >= 1 && monthVal <= 12) {
                            String label = Month.of(monthVal).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                            dataMap.put(label, new BigDecimal(row[2].toString()));
                        }
                    } catch (Exception e) {
                        log.warn("Error parsing monthly revenue row: {}", e.getMessage());
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

    private List<SuperAdminAnalyticsDTO.SubscriberTrendDTO> formatWeeklySubscribers(List<Object[]> data) {
        Map<Integer, Long> dataMap = new HashMap<>();
        if (data != null) {
            for (Object[] row : data) {
                if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                    try {
                        int dow = ((Number) row[0]).intValue();
                        long count = ((Number) row[1]).longValue();
                        dataMap.put(dow, count);
                    } catch (Exception e) {
                        log.warn("Error parsing weekly subscriber row: {}", e.getMessage());
                    }
                }
            }
        }

        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        List<SuperAdminAnalyticsDTO.SubscriberTrendDTO> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            result.add(new SuperAdminAnalyticsDTO.SubscriberTrendDTO(days[i], dataMap.getOrDefault(i, 0L)));
        }
        return result;
    }

    private List<SuperAdminAnalyticsDTO.SubscriberTrendDTO> formatMonthlySubscribers(List<Object[]> data) {
        Map<String, Long> dataMap = new HashMap<>();
        if (data != null) {
            for (Object[] row : data) {
                if (row != null && row.length >= 3 && row[1] != null && row[2] != null) {
                    try {
                        int monthVal = ((Number) row[1]).intValue();
                        if (monthVal >= 1 && monthVal <= 12) {
                            String label = Month.of(monthVal).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                            dataMap.put(label, ((Number) row[2]).longValue());
                        }
                    } catch (Exception e) {
                        log.warn("Error parsing monthly subscriber row: {}", e.getMessage());
                    }
                }
            }
        }

        List<SuperAdminAnalyticsDTO.SubscriberTrendDTO> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 5; i >= 0; i--) {
            String label = now.minusMonths(i).getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            result.add(new SuperAdminAnalyticsDTO.SubscriberTrendDTO(label, dataMap.getOrDefault(label, 0L)));
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
                log.warn("Error parsing top station row: {}", e.getMessage());
                return new SuperAdminAnalyticsDTO.TopStationDTO("Unknown", BigDecimal.ZERO, "Rs 0");
            }
        }).collect(Collectors.toList());
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
