package com.fixzone.fixzon_backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsDTO {
    private UUID id;
    private String metrics;
    private LocalDate date;
    private UUID serviceCenterId;
    
    // Fields for company analytics
    private BigDecimal totalRevenue;
    private String revenueChange;
    private long totalJobs;
    private String jobsChange;
    private long pendingJobs;
    private String pendingJobsChange;
    private BigDecimal avgJobValue;
    private String avgJobValueChange;
    private String updatedAt;
    private BigDecimal onlineRevenue;
    private BigDecimal handCollectionRevenue;
    private List<MonthlyDataDTO> revenueOverview;
    private List<MonthlyGrowthDTO> customerGrowth;
    private List<ServiceBreakdownDTO> serviceBreakdown;
    private List<CenterPerformanceDTO> topCenters;

    // Alternative constructor for AnalyticsService usage
    public AnalyticsDTO(BigDecimal totalRevenue, String revenueChange, long totalJobs, String jobsChange,
                        long pendingJobs, String pendingJobsChange, BigDecimal avgJobValue, 
                        String avgJobValueChange, String updatedAt, BigDecimal onlineRevenue, 
                        BigDecimal handCollectionRevenue, List<MonthlyDataDTO> revenueOverview,
                        List<MonthlyGrowthDTO> customerGrowth, List<ServiceBreakdownDTO> serviceBreakdown,
                        List<CenterPerformanceDTO> topCenters) {
        this.totalRevenue = totalRevenue;
        this.revenueChange = revenueChange;
        this.totalJobs = totalJobs;
        this.jobsChange = jobsChange;
        this.pendingJobs = pendingJobs;
        this.pendingJobsChange = pendingJobsChange;
        this.avgJobValue = avgJobValue;
        this.avgJobValueChange = avgJobValueChange;
        this.updatedAt = updatedAt;
        this.onlineRevenue = onlineRevenue;
        this.handCollectionRevenue = handCollectionRevenue;
        this.revenueOverview = revenueOverview;
        this.customerGrowth = customerGrowth;
        this.serviceBreakdown = serviceBreakdown;
        this.topCenters = topCenters;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyDataDTO {
        private String name;
        private BigDecimal revenue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyGrowthDTO {
        private String name;
        private int newCustomers;
        private int activeCustomers;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceBreakdownDTO {
        private String name;
        private int value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CenterPerformanceDTO {
        private String id;
        private String name;
        private String initial;
        private String color;
        private int jobs;
        private BigDecimal revenue;
    }
}
