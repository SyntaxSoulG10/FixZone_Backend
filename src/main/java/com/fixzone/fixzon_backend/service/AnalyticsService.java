package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.AnalyticsDTO;
import com.fixzone.fixzon_backend.model.Booking;
import com.fixzone.fixzon_backend.model.Invoice;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.model.ServicePackage;
import com.fixzone.fixzon_backend.repository.BookingRepository;
import com.fixzone.fixzon_backend.repository.InvoiceRepository;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.ServicePackageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
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

        @Autowired
        private com.fixzone.fixzon_backend.repository.CustomerRepository customerRepository;

        public AnalyticsDTO getCompanyAnalytics(String companyCode) {
                // Fetch all data for this company (based on companyCode in invoices)
                List<Invoice> invoices = invoiceRepository.findByCompanyCode(companyCode);

                // Stats Calculation
                BigDecimal totalRevenue = invoices.stream()
                                .filter(i -> "PAID".equalsIgnoreCase(i.getStatus()))
                                .map(Invoice::getTotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Fetch bookings (associated with these centers)
                Set<UUID> centerIds = invoices.stream().map(Invoice::getCenterId).collect(Collectors.toSet());
                List<Booking> bookings = bookingRepository.findAll().stream()
                                .filter(b -> centerIds.contains(b.getCenterId()))
                                .collect(Collectors.toList());

                long totalJobs = bookings.size();
                long pendingJobs = bookings.stream().filter(b -> "PENDING".equalsIgnoreCase(b.getStatus())).count();

                BigDecimal avgJobValue = totalJobs > 0
                                ? totalRevenue.divide(BigDecimal.valueOf(totalJobs), 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;

                // Time-based calculations for "Changes"
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime firstDayCurrentMonth = now.withDayOfMonth(1).withHour(0).withMinute(0);
                LocalDateTime firstDayLastMonth = firstDayCurrentMonth.minusMonths(1);

                BigDecimal currentMonthRevenue = invoices.stream()
                                .filter(i -> "PAID".equalsIgnoreCase(i.getStatus())
                                                && i.getIssuedAt().isAfter(firstDayCurrentMonth))
                                .map(Invoice::getTotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal lastMonthRevenue = invoices.stream()
                                .filter(i -> "PAID".equalsIgnoreCase(i.getStatus())
                                                && i.getIssuedAt().isAfter(firstDayLastMonth)
                                                && i.getIssuedAt().isBefore(firstDayCurrentMonth))
                                .map(Invoice::getTotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                String revenueChange = calculatePercentageChange(currentMonthRevenue, lastMonthRevenue);

                long currentMonthJobs = bookings.stream()
                                .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().isAfter(firstDayCurrentMonth))
                                .count();
                long lastMonthJobs = bookings.stream()
                                .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().isAfter(firstDayLastMonth)
                                                && b.getCreatedAt().isBefore(firstDayCurrentMonth))
                                .count();
                String jobsChange = calculatePercentageChange(BigDecimal.valueOf(currentMonthJobs),
                                BigDecimal.valueOf(lastMonthJobs));

                // Comparison for Pending Jobs Change (Current vs 7 days ago snapshot)
                long pendingOld = bookings.stream()
                                .filter(b -> "PENDING".equalsIgnoreCase(b.getStatus()) && b.getCreatedAt() != null
                                                && b.getCreatedAt().isBefore(now.minusDays(7)))
                                .count();
                String pendingJobsChange = calculatePercentageChange(BigDecimal.valueOf(pendingJobs),
                                BigDecimal.valueOf(pendingOld));

                // Comparison for Avg Job Value Change
                BigDecimal currentAvg = currentMonthJobs > 0
                                ? currentMonthRevenue.divide(BigDecimal.valueOf(currentMonthJobs), 2,
                                                RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;
                BigDecimal lastAvg = lastMonthJobs > 0 ? lastMonthRevenue.divide(BigDecimal.valueOf(lastMonthJobs), 2,
                                RoundingMode.HALF_UP) : BigDecimal.ZERO;
                String avgJobValueChange = calculatePercentageChange(currentAvg, lastAvg);

                // Revenue Overview (Last 6 Months, sorted)
                List<AnalyticsDTO.MonthlyDataDTO> revenueOverview = invoices.stream()
                                .filter(i -> "PAID".equalsIgnoreCase(i.getStatus()))
                                .collect(Collectors.groupingBy(
                                                i -> i.getIssuedAt().getYear() * 100 + i.getIssuedAt().getMonthValue(),
                                                TreeMap::new,
                                                Collectors.reducing(BigDecimal.ZERO, Invoice::getTotal,
                                                                BigDecimal::add)))
                                .entrySet().stream()
                                .map(entry -> {
                                        int yearMonth = entry.getKey();
                                        String monthName = java.time.Month.of(yearMonth % 100)
                                                        .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                                        return new AnalyticsDTO.MonthlyDataDTO(monthName, entry.getValue());
                                })
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
                                                        "#EA580C",
                                                        (int) jobsCount,
                                                        revenue);
                                })
                                .filter(Objects::nonNull)
                                .sorted((c1, c2) -> c2.getRevenue().compareTo(c1.getRevenue()))
                                .limit(5)
                                .collect(Collectors.toList());

                // Customer Growth - Real implementation
                List<AnalyticsDTO.MonthlyGrowthDTO> customerGrowth = bookings.stream()
                                .filter(b -> b.getCreatedAt() != null)
                                .collect(Collectors.groupingBy(
                                                b -> b.getCreatedAt().getYear() * 100 + b.getCreatedAt().getMonthValue(),
                                                TreeMap::new,
                                                Collectors.toSet()))
                                .entrySet().stream()
                                .map(entry -> {
                                        int yearMonth = entry.getKey();
                                        int year = yearMonth / 100;
                                        int monthOrdinal = yearMonth % 100;
                                        LocalDateTime start = LocalDateTime.of(year, monthOrdinal, 1, 0, 0);
                                        LocalDateTime end = start.plusMonths(1);

                                        String monthName = java.time.Month.of(monthOrdinal)
                                                        .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

                                        int active = entry.getValue().stream().map(Booking::getCustomerId)
                                                        .collect(Collectors.toSet()).size();

                                        long newCust = customerRepository.findAll().stream()
                                                        .filter(c -> c.getCreatedAt() != null
                                                                        && c.getCreatedAt().isAfter(start)
                                                                        && c.getCreatedAt().isBefore(end))
                                                        .count();

                                        return new AnalyticsDTO.MonthlyGrowthDTO(monthName, (int) newCust, active);
                                })
                                .collect(Collectors.toList());

                // Service Breakdown
                List<AnalyticsDTO.ServiceBreakdownDTO> serviceBreakdown = bookings.stream()
                                .filter(b -> b.getPackageId() != null)
                                .collect(Collectors.groupingBy(Booking::getPackageId, Collectors.counting()))
                                .entrySet().stream()
                                .map(entry -> {
                                        String packageName = servicePackageRepository.findById(entry.getKey())
                                                        .map(ServicePackage::getName).orElse("General Service");
                                        return new AnalyticsDTO.ServiceBreakdownDTO(packageName,
                                                        entry.getValue().intValue());
                                })
                                .collect(Collectors.toList());

                String updatedAt = java.time.LocalDateTime.now()
                                .format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"));

                return new AnalyticsDTO(totalRevenue, revenueChange, totalJobs, jobsChange, pendingJobs,
                                pendingJobsChange,
                                avgJobValue, avgJobValueChange, updatedAt,
                                revenueOverview, customerGrowth, serviceBreakdown, topCenters);
        }

        private String calculatePercentageChange(BigDecimal current, BigDecimal previous) {
                if (previous.compareTo(BigDecimal.ZERO) == 0) {
                        return current.compareTo(BigDecimal.ZERO) > 0 ? "+100%" : "0.0%";
                }
                BigDecimal change = current.subtract(previous)
                                .divide(previous, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(1, RoundingMode.HALF_UP);
                return (change.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + change.toString() + "%";
        }
}
