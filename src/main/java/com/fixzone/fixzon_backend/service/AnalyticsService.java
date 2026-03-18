package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.AnalyticsDTO;
import com.fixzone.fixzon_backend.model.Booking;
import com.fixzone.fixzon_backend.model.Invoice;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.repository.BookingRepository;
import com.fixzone.fixzon_backend.repository.InvoiceRepository;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.ServicePackageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

        @Autowired
        private InvoiceRepository invoiceRepository;

        @Autowired
        private BookingRepository bookingRepository;

        @Autowired
        private ServiceCenterRepository serviceCenterRepository;

        @Autowired
        private ServicePackageRepository servicePackageRepository;

        public AnalyticsDTO getCompanyAnalytics(String companyCode) {
                // Fetch all data for this company (based on companyCode in invoices)
                List<Invoice> invoices = invoiceRepository.findByCompanyCode(companyCode);

                // Stats Calculation
                BigDecimal totalRevenue = invoices.stream()
                                .filter(i -> "PAID".equalsIgnoreCase(i.getStatus()))
                                .map(Invoice::getTotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Fetch bookings (assuming they are linked via companyCode or centerId)
                // Since Booking doesn't have companyCode, we'll fetch all and filter by
                // centerIds associated with these invoices
                Set<UUID> centerIds = invoices.stream().map(Invoice::getCenterId).collect(Collectors.toSet());
                List<Booking> bookings = bookingRepository.findAll().stream()
                                .filter(b -> centerIds.contains(b.getCenterId()))
                                .collect(Collectors.toList());

                long totalJobs = bookings.size();
                long pendingJobs = bookings.stream().filter(b -> "PENDING".equalsIgnoreCase(b.getStatus())).count();

                BigDecimal avgJobValue = totalJobs > 0
                                ? totalRevenue.divide(BigDecimal.valueOf(totalJobs), 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;

                // Revenue Overview (Last 6 Months)
                List<AnalyticsDTO.MonthlyDataDTO> revenueOverview = invoices.stream()
                                .filter(i -> "PAID".equalsIgnoreCase(i.getStatus()))
                                .collect(Collectors.groupingBy(
                                                i -> i.getIssuedAt().getMonth().getDisplayName(TextStyle.SHORT,
                                                                Locale.ENGLISH),
                                                LinkedHashMap::new,
                                                Collectors.mapping(Invoice::getTotal,
                                                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))))
                                .entrySet().stream()
                                .map(entry -> new AnalyticsDTO.MonthlyDataDTO(entry.getKey(), entry.getValue()))
                                .limit(7)
                                .collect(Collectors.toList());

                // Top Centers
                List<AnalyticsDTO.CenterPerformanceDTO> topCenters = centerIds.stream()
                                .map(centerId -> {
                                        ServiceCenter center = serviceCenterRepository.findById(centerId).orElse(null);
                                        if (center == null)
                                                return null;

                                        long jobsCount = bookings.stream().filter(b -> b.getCenterId().equals(centerId))
                                                        .count();
                                        BigDecimal revenue = invoices.stream()
                                                        .filter(i -> i.getCenterId().equals(centerId)
                                                                        && "PAID".equalsIgnoreCase(i.getStatus()))
                                                        .map(Invoice::getTotal)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                        return new AnalyticsDTO.CenterPerformanceDTO(
                                                        centerId.toString(),
                                                        center.getName(),
                                                        center.getName().substring(0, 1).toUpperCase(),
                                                        "primary.main", // default color
                                                        (int) jobsCount,
                                                        revenue);
                                })
                                .filter(Objects::nonNull)
                                .sorted((c1, c2) -> c2.getRevenue().compareTo(c1.getRevenue()))
                                .limit(5)
                                .collect(Collectors.toList());

                // Customer Growth (Hardcoded mock for now as we don't have enough history data
                // in the DB model easily)
                List<AnalyticsDTO.MonthlyGrowthDTO> customerGrowth = Arrays.asList(
                                new AnalyticsDTO.MonthlyGrowthDTO("Jan", 20, 40),
                                new AnalyticsDTO.MonthlyGrowthDTO("Feb", 30, 60),
                                new AnalyticsDTO.MonthlyGrowthDTO("Mar", 45, 90),
                                new AnalyticsDTO.MonthlyGrowthDTO("Apr", 25, 100),
                                new AnalyticsDTO.MonthlyGrowthDTO("May", 60, 150),
                                new AnalyticsDTO.MonthlyGrowthDTO("Jun", 80, 210));

                // Service Breakdown (Group by packageId and get names from
                // ServicePackageRepository)
                List<AnalyticsDTO.ServiceBreakdownDTO> serviceBreakdown = bookings.stream()
                                .filter(b -> b.getPackageId() != null)
                                .collect(Collectors.groupingBy(Booking::getPackageId, Collectors.counting()))
                                .entrySet().stream()
                                .map(entry -> {
                                        String packageName = servicePackageRepository.findById(entry.getKey())
                                                        .map(p -> p.getName()).orElse("Unknown");
                                        return new AnalyticsDTO.ServiceBreakdownDTO(packageName,
                                                        entry.getValue().intValue());
                                })
                                .collect(Collectors.toList());

                // Calculate/Mock changes
                String revenueChange = "+15%";
                String jobsChange = "+3.2%";
                String pendingJobsChange = "-5%";
                String avgJobValueChange = "+2.1%";
                String updatedAt = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"));

                return new AnalyticsDTO(
                                totalRevenue, revenueChange, totalJobs, jobsChange, pendingJobs, pendingJobsChange,
                                avgJobValue, avgJobValueChange, updatedAt,
                                revenueOverview, customerGrowth, serviceBreakdown, topCenters);
        }
}
