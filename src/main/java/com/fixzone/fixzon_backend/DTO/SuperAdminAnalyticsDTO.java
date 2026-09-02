package com.fixzone.fixzon_backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data 
@NoArgsConstructor 
@AllArgsConstructor
public class SuperAdminAnalyticsDTO {
    // Stat Cards
    private BigDecimal totalPlatformRevenue;
    private String revenueChange;          // e.g., "+15.3%"
    private long totalServiceCenters;
    private long pendingRegistrations;
    private long activeSubscriptions;
    private String subscriptionChange;     // e.g., "+5.7%"

    // Revenue Charts (from subscription_billing)
    private List<RevenueBarDTO> weeklyRevenue;
    private List<RevenueBarDTO> monthlyRevenue;

    // Subscriber Trend Charts (new subscribers per day/month)
    private List<SubscriberTrendDTO> weeklySubscribers;
    private List<SubscriberTrendDTO> monthlySubscribers;

    // Tables
    private List<TopStationDTO> topStations;

    @Data 
    @NoArgsConstructor 
    @AllArgsConstructor
    public static class RevenueBarDTO {
        private String label;          // "Mon", "Jan", etc.
        private BigDecimal amount;
        private int percentage;        // Relative to max bar
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscriberTrendDTO {
        private String label;          // "Mon", "Jan", etc.
        private long count;            // Number of new subscribers
    }

    @Data 
    @NoArgsConstructor 
    @AllArgsConstructor
    public static class TopStationDTO {
        private String name;
        private BigDecimal revenue;
        private String formattedRevenue; // e.g., "Rs 4.2M"
    }
}
