package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.AnalyticsDTO;
import com.fixzone.fixzon_backend.enums.BookingStatus;
import com.fixzone.fixzon_backend.model.Booking;
import com.fixzone.fixzon_backend.model.Invoice;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.model.ServicePackage;
import com.fixzone.fixzon_backend.repository.BookingRepository;
import com.fixzone.fixzon_backend.repository.InvoiceRepository;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.ServicePackageRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

        private final InvoiceRepository invoiceRepository;
        private final BookingRepository bookingRepository;
        private final ServiceCenterRepository serviceCenterRepository;
        private final ServicePackageRepository servicePackageRepository;
        private final com.fixzone.fixzon_backend.repository.CustomerRepository customerRepository;
        private final com.fixzone.fixzon_backend.repository.PaymentRecordRepository paymentRecordRepository;

        private final com.fixzone.fixzon_backend.repository.OwnerRepository ownerRepository;

        public AnalyticsService(InvoiceRepository invoiceRepository,
                                BookingRepository bookingRepository,
                                ServiceCenterRepository serviceCenterRepository,
                                ServicePackageRepository servicePackageRepository,
                                com.fixzone.fixzon_backend.repository.CustomerRepository customerRepository,
                                com.fixzone.fixzon_backend.repository.PaymentRecordRepository paymentRecordRepository,
                                com.fixzone.fixzon_backend.repository.OwnerRepository ownerRepository) {
                this.invoiceRepository = invoiceRepository;
                this.bookingRepository = bookingRepository;
                this.serviceCenterRepository = serviceCenterRepository;
                this.servicePackageRepository = servicePackageRepository;
                this.customerRepository = customerRepository;
                this.paymentRecordRepository = paymentRecordRepository;
                this.ownerRepository = ownerRepository;
        }

        public AnalyticsDTO getCompanyAnalytics(String companyCode, String centerIdStr, String startDateStr, String endDateStr, String period) {
                // Parse filters
                UUID filterCenterId = (centerIdStr != null && !centerIdStr.equals("all") && !centerIdStr.isEmpty())
                                ? UUID.fromString(centerIdStr)
                                : null;
                LocalDateTime startFilter = (startDateStr != null && !startDateStr.isEmpty())
                                ? java.time.LocalDate.parse(startDateStr).atStartOfDay()
                                : null;
                LocalDateTime endFilter = (endDateStr != null && !endDateStr.isEmpty())
                                ? java.time.LocalDate.parse(endDateStr).atTime(23, 59, 59)
                                : null;

                // Fetch all data for this company (based on companyCode in invoices)
                List<Invoice> allInvoices = invoiceRepository.findByCompanyCode(companyCode);

                // Fetch all centers belonging to this owner to ensure we show even those with 0 revenue
                Set<UUID> centerIds;
                if (filterCenterId != null) {
                    centerIds = Set.of(filterCenterId);
                } else {
                    centerIds = ownerRepository.findByOwnerCode(companyCode)
                            .map(owner -> serviceCenterRepository.findByOwner_UserId(owner.getUserId()))
                            .map(list -> list.stream().map(com.fixzone.fixzon_backend.model.ServiceCenter::getCenterId).collect(Collectors.toSet()))
                            .orElseGet(() -> allInvoices.stream().map(Invoice::getCenterId).collect(Collectors.toSet()));
                }

                // Apply Filters to Invoices
                List<Invoice> invoices = allInvoices.stream()
                                .filter(i -> filterCenterId == null || i.getCenterId().equals(filterCenterId))
                                .filter(i -> startFilter == null || i.getIssuedAt().isAfter(startFilter) || i.getIssuedAt().isEqual(startFilter))
                                .filter(i -> endFilter == null || i.getIssuedAt().isBefore(endFilter) || i.getIssuedAt().isEqual(endFilter))
                                .collect(Collectors.toList());

                // Stats Calculation
                BigDecimal totalRevenue = invoices.stream()
                                .filter(i -> "PAID".equalsIgnoreCase(i.getStatus()))
                                .map(Invoice::getTotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                List<Booking> allBookings = bookingRepository.findByCenterIdIn(centerIds);
                
                // Apply Filters to Bookings
                List<Booking> bookings = allBookings.stream()
                                .filter(b -> startFilter == null || b.getCreatedAt().isAfter(startFilter) || b.getCreatedAt().isEqual(startFilter))
                                .filter(b -> endFilter == null || b.getCreatedAt().isBefore(endFilter) || b.getCreatedAt().isEqual(endFilter))
                                .collect(Collectors.toList());

                long totalJobs = bookings.size();
                long pendingJobs = bookings.stream().filter(b -> (b.getStatus() == BookingStatus.CONFIRMED)).count();

                BigDecimal avgJobValue = totalJobs > 0
                                ? totalRevenue.divide(BigDecimal.valueOf(totalJobs), 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;

                // Split Revenue by Payment Method
                List<com.fixzone.fixzon_backend.model.PaymentRecord> allPayments = centerIds.stream()
                                .flatMap(centerId -> paymentRecordRepository.findByCenterId(centerId).stream())
                                .filter(p -> startFilter == null || p.getCreatedAt().isAfter(startFilter) || p.getCreatedAt().isEqual(startFilter))
                                .filter(p -> endFilter == null || p.getCreatedAt().isBefore(endFilter) || p.getCreatedAt().isEqual(endFilter))
                                .collect(Collectors.toList());

                BigDecimal onlineRevenue = allPayments.stream()
                                .filter(p -> "CARD".equalsIgnoreCase(p.getMethod()) || "ONLINE".equalsIgnoreCase(p.getMethod()))
                                .map(com.fixzone.fixzon_backend.model.PaymentRecord::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal handCollectionRevenue = allPayments.stream()
                                .filter(p -> "CASH".equalsIgnoreCase(p.getMethod()))
                                .map(com.fixzone.fixzon_backend.model.PaymentRecord::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

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
                                .filter(b -> (b.getStatus() == BookingStatus.CONFIRMED) && b.getCreatedAt() != null
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

                // Revenue Overview (Daily, Monthly, Yearly grouping)
                List<AnalyticsDTO.MonthlyDataDTO> revenueOverview = invoices.stream()
                                .filter(i -> "PAID".equalsIgnoreCase(i.getStatus()))
                                .collect(Collectors.groupingBy(
                                                i -> {
                                                    if ("daily".equalsIgnoreCase(period)) {
                                                        return i.getIssuedAt().getYear() * 10000 + i.getIssuedAt().getMonthValue() * 100 + i.getIssuedAt().getDayOfMonth();
                                                    } else if ("yearly".equalsIgnoreCase(period)) {
                                                        return i.getIssuedAt().getYear();
                                                    } else {
                                                        return i.getIssuedAt().getYear() * 100 + i.getIssuedAt().getMonthValue();
                                                    }
                                                },
                                                TreeMap::new,
                                                Collectors.toList()))
                                .entrySet().stream()
                                .map(entry -> {
                                        int key = entry.getKey();
                                        String name;
                                        LocalDateTime start;
                                        LocalDateTime end;

                                        if ("daily".equalsIgnoreCase(period)) {
                                            int year = key / 10000;
                                            int month = (key % 10000) / 100;
                                            int day = key % 100;
                                            start = LocalDateTime.of(year, month, day, 0, 0);
                                            end = start.plusDays(1);
                                            name = day + " " + java.time.Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                                        } else if ("yearly".equalsIgnoreCase(period)) {
                                            start = LocalDateTime.of(key, 1, 1, 0, 0);
                                            end = start.plusYears(1);
                                            name = String.valueOf(key);
                                        } else {
                                            int year = key / 100;
                                            int month = key % 100;
                                            start = LocalDateTime.of(year, month, 1, 0, 0);
                                            end = start.plusMonths(1);
                                            name = java.time.Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                                        }
                                        
                                        List<Invoice> groupedInvoices = entry.getValue();
                                        BigDecimal total = groupedInvoices.stream().map(Invoice::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
                                        
                                        BigDecimal online = allPayments.stream()
                                                        .filter(p -> p.getCreatedAt().isAfter(start) && p.getCreatedAt().isBefore(end))
                                                        .filter(p -> "CARD".equalsIgnoreCase(p.getMethod()) || "ONLINE".equalsIgnoreCase(p.getMethod()))
                                                        .map(com.fixzone.fixzon_backend.model.PaymentRecord::getAmount)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        
                                        BigDecimal cash = allPayments.stream()
                                                        .filter(p -> p.getCreatedAt().isAfter(start) && p.getCreatedAt().isBefore(end))
                                                        .filter(p -> "CASH".equalsIgnoreCase(p.getMethod()))
                                                        .map(com.fixzone.fixzon_backend.model.PaymentRecord::getAmount)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                        return new AnalyticsDTO.MonthlyDataDTO(name, total, online, cash);
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

                                        long newCust = customerRepository.countByCreatedAtBetween(start, end);

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
                                avgJobValue, avgJobValueChange, updatedAt, onlineRevenue, handCollectionRevenue,
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
