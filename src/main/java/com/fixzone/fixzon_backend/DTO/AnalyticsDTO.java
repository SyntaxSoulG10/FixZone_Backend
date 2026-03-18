package com.fixzone.fixzon_backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsDTO {
    private BigDecimal totalRevenue;
    private String revenueChange;
    private long totalJobs;
    private String jobsChange;
    private long pendingJobs;
    private String pendingJobsChange;
    private BigDecimal avgJobValue;
    private String avgJobValueChange;
    private String updatedAt;
    private List<MonthlyDataDTO> revenueOverview;
    private List<MonthlyGrowthDTO> customerGrowth;
    private List<ServiceBreakdownDTO> serviceBreakdown;
    private List<CenterPerformanceDTO> topCenters;

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
