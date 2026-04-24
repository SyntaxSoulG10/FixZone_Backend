package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.AnalyticsDTO;
import com.fixzone.fixzon_backend.enums.BookingStatus;
import com.fixzone.fixzon_backend.model.*;
import com.fixzone.fixzon_backend.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for calculating business performance metrics.
 * It combines data from various sources like invoices and bookings 
 * to provide a clear overview for company owners.
 */
@Service
public class AnalyticsService {

    private final InvoiceRepository invoiceRepository;
    private final BookingRepository bookingRepository;
    private final ServiceCenterRepository serviceCenterRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final CustomerRepository customerRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final OwnerRepository ownerRepository;

    // We use constants for percentages and formatting to avoid "magic numbers" in our logic.
    private static final String POSITIVE_PREFIX = "+";
    private static final String PERCENT_SUFFIX = "%";
    private static final String ZERO_PERCENT = "0.0%";

    public AnalyticsService(InvoiceRepository invoiceRepository,
                            BookingRepository bookingRepository,
                            ServiceCenterRepository serviceCenterRepository,
                            ServicePackageRepository servicePackageRepository,
                            CustomerRepository customerRepository,
                            PaymentRecordRepository paymentRecordRepository,
                            OwnerRepository ownerRepository) {
        this.invoiceRepository = invoiceRepository;
        this.bookingRepository = bookingRepository;
        this.serviceCenterRepository = serviceCenterRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.customerRepository = customerRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.ownerRepository = ownerRepository;
    }

    /**
     * Aggregates all analytics data for a specific company.
     * We use descriptive parameter names and clear logic flow to make it easy to follow.
     */
    public AnalyticsDTO getCompanyAnalytics(String companyCode, String centerIdStr, String startDateStr, String endDateStr, String period) {
        
        // --- STEP 1: Parse and Prepare Filters ---
        // We convert the input strings into meaningful Java objects like UUID and LocalDateTime.
        // This ensures that all following comparisons are type-safe and consistent.
        UUID filterCenterId = parseCenterId(centerIdStr);
        LocalDateTime startRange = parseStartDate(startDateStr);
        LocalDateTime endRange = parseEndDate(endDateStr);

        // --- STEP 2: Fetch and Filter Data ---
        // We fetch the baseline data from the DB first, then apply our business filters.
        List<Invoice> companyInvoices = invoiceRepository.findByCompanyCode(companyCode);
        Set<UUID> targetCenterIds = getTargetCenters(companyCode, filterCenterId, companyInvoices);

        // Filter invoices based on date and center selection for the current report view
        List<Invoice> filteredInvoices = companyInvoices.stream()
                .filter(inv -> filterCenterId == null || inv.getCenterId().equals(filterCenterId))
                .filter(inv -> startRange == null || !inv.getIssuedAt().isBefore(startRange))
                .filter(inv -> endRange == null || !inv.getIssuedAt().isAfter(endRange))
                .collect(Collectors.toList());

        // --- STEP 3: Calculate Key Stats ---
        // These are the "Big Numbers" shown at the top of the dashboard.
        // We only count 'PAID' invoices for revenue to accurately reflect the money actually received.
        BigDecimal totalRevenue = filteredInvoices.stream()
                .filter(inv -> "PAID".equalsIgnoreCase(inv.getStatus()))
                .map(Invoice::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // We fetch bookings for the selected centers to calculate job-related metrics.
        List<Booking> companyBookings = bookingRepository.findByCenterIdIn(targetCenterIds);
        
        // We use bookingDate (actual service date) to filter jobs, making the "Today" view accurate.
        List<Booking> filteredBookings = companyBookings.stream()
                .filter(b -> startRange == null || !b.getBookingDate().isBefore(startRange.toLocalDate()))
                .filter(b -> endRange == null || !b.getBookingDate().isAfter(endRange.toLocalDate()))
                .collect(Collectors.toList());

        long totalJobs = filteredBookings.size();
        long pendingJobs = filteredBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .count();

        BigDecimal averageValue = totalJobs > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalJobs), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // --- STEP 4: Calculate Growth Indicators ---
        // This logic compares current performance with the previous month to show trends.
        // It's a standard business practice to measure month-over-month growth.
        AnalyticsDTO.GrowthStats trends = getGrowthTrends(companyInvoices, companyBookings, pendingJobs);

        // --- STEP 5: Prepare Charts and Breakdowns ---
        // We transform the raw data into DTOs that the frontend can easily map to UI charts.
        List<PaymentRecord> payments = getFilteredPayments(targetCenterIds, startRange, endRange);
        List<AnalyticsDTO.MonthlyDataDTO> revenueChart = getRevenueChart(filteredInvoices, payments, period);
        List<AnalyticsDTO.MonthlyGrowthDTO> customerChart = getCustomerChart(companyCode, filteredInvoices, period);
        List<AnalyticsDTO.ServiceBreakdownDTO> serviceMix = getServiceMix(filteredBookings);
        List<AnalyticsDTO.CenterPerformanceDTO> centerRankings = getCenterRankings(targetCenterIds, filteredInvoices, filteredBookings);

        // We record the exact time of the update for the "Last updated" label in the UI.
        String updateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"));

        return new AnalyticsDTO(
                totalRevenue, trends.getRevenueChange(),
                totalJobs, trends.getJobsChange(),
                pendingJobs, trends.getPendingChange(),
                averageValue, trends.getAverageValueChange(),
                updateTime, sumPayments(payments, "CARD", "ONLINE"), sumPayments(payments, "CASH"),
                revenueChart, customerChart, serviceMix, centerRankings
        );
    }

    // --- Helper Methods to separate logic (Separation of Concerns) ---

    private UUID parseCenterId(String str) {
        return (str != null && !"all".equalsIgnoreCase(str) && !str.isEmpty()) ? UUID.fromString(str) : null;
    }

    private LocalDateTime parseStartDate(String str) {
        return (str != null && !str.isEmpty()) ? LocalDate.parse(str).atStartOfDay() : null;
    }

    private LocalDateTime parseEndDate(String str) {
        return (str != null && !str.isEmpty()) ? LocalDate.parse(str).atTime(23, 59, 59) : null;
    }

    /**
     * Determines which service centers we should look at based on the filter.
     * If 'All' is selected, we include every center owned by the company.
     */
    private Set<UUID> getTargetCenters(String code, UUID filterId, List<Invoice> invoices) {
        if (filterId != null) return Set.of(filterId);
        return ownerRepository.findByOwnerCode(code)
                .map(owner -> serviceCenterRepository.findByOwner_UserId(owner.getUserId()))
                .map(list -> list.stream().map(ServiceCenter::getCenterId).collect(Collectors.toSet()))
                .orElseGet(() -> invoices.stream().map(Invoice::getCenterId).collect(Collectors.toSet()));
    }

    private List<PaymentRecord> getFilteredPayments(Set<UUID> ids, LocalDateTime start, LocalDateTime end) {
        return ids.stream()
                .flatMap(id -> paymentRecordRepository.findByCenterId(id).stream())
                .filter(p -> start == null || !p.getCreatedAt().isBefore(start))
                .filter(p -> end == null || !p.getCreatedAt().isAfter(end))
                .collect(Collectors.toList());
    }

    private BigDecimal sumPayments(List<PaymentRecord> payments, String... methods) {
        Set<String> set = Set.of(methods);
        return payments.stream()
                .filter(p -> set.contains(p.getMethod().toUpperCase()))
                .map(PaymentRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates percentage changes for various KPIs to show trends.
     * We use a 7-day window for pending jobs to show weekly progress.
     */
    private AnalyticsDTO.GrowthStats getGrowthTrends(List<Invoice> invoices, List<Booking> bookings, long pending) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0);
        LocalDateTime lastMonthStart = monthStart.minusMonths(1);

        BigDecimal curRev = invoices.stream().filter(i -> "PAID".equalsIgnoreCase(i.getStatus()) && !i.getIssuedAt().isBefore(monthStart)).map(Invoice::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal lastRev = invoices.stream().filter(i -> "PAID".equalsIgnoreCase(i.getStatus()) && !i.getIssuedAt().isBefore(lastMonthStart) && i.getIssuedAt().isBefore(monthStart)).map(Invoice::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long curJobs = bookings.stream().filter(b -> b.getBookingDate() != null && !b.getBookingDate().isBefore(monthStart.toLocalDate())).count();
        long lastJobs = bookings.stream().filter(b -> b.getBookingDate() != null && !b.getBookingDate().isBefore(lastMonthStart.toLocalDate()) && b.getBookingDate().isBefore(monthStart.toLocalDate())).count();
        
        long oldPending = bookings.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED && b.getBookingDate() != null && b.getBookingDate().isBefore(now.toLocalDate().minusDays(7))).count();
        
        BigDecimal curAvg = curJobs > 0 ? curRev.divide(BigDecimal.valueOf(curJobs), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal lastAvg = lastJobs > 0 ? lastRev.divide(BigDecimal.valueOf(lastJobs), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        return new AnalyticsDTO.GrowthStats(
                calculatePercentageChange(curRev, lastRev),
                calculatePercentageChange(BigDecimal.valueOf(curJobs), BigDecimal.valueOf(lastJobs)),
                calculatePercentageChange(BigDecimal.valueOf(pending), BigDecimal.valueOf(oldPending)),
                calculatePercentageChange(curAvg, lastAvg)
        );
    }

    private List<AnalyticsDTO.MonthlyDataDTO> getRevenueChart(List<Invoice> invoices, List<PaymentRecord> payments, String period) {
        return invoices.stream()
                .filter(i -> "PAID".equalsIgnoreCase(i.getStatus()))
                .collect(Collectors.groupingBy(i -> getTimeKey(i.getIssuedAt(), period), TreeMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(entry -> {
                    String label = formatLabel(entry.getKey(), period);
                    LocalDateTime[] range = getRange(entry.getKey(), period);
                    BigDecimal total = entry.getValue().stream().map(Invoice::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal online = payments.stream().filter(p -> !p.getCreatedAt().isBefore(range[0]) && p.getCreatedAt().isBefore(range[1]) && Set.of("CARD", "ONLINE").contains(p.getMethod().toUpperCase())).map(PaymentRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal cash = payments.stream().filter(p -> !p.getCreatedAt().isBefore(range[0]) && p.getCreatedAt().isBefore(range[1]) && "CASH".equalsIgnoreCase(p.getMethod())).map(PaymentRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new AnalyticsDTO.MonthlyDataDTO(label, total, online, cash);
                })
                .collect(Collectors.toList());
    }

    private List<AnalyticsDTO.MonthlyGrowthDTO> getCustomerChart(String code, List<Invoice> invoices, String period) {
        Set<UUID> ids = invoiceRepository.findByCompanyCode(code).stream().map(Invoice::getIssuedToCustomerId).collect(Collectors.toSet());
        List<Customer> customers = customerRepository.findAllById(ids);

        return invoices.stream()
                .collect(Collectors.groupingBy(i -> getTimeKey(i.getIssuedAt(), period), TreeMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(entry -> {
                    String label = formatLabel(entry.getKey(), period);
                    LocalDateTime[] range = getRange(entry.getKey(), period);
                    long newCust = customers.stream().filter(c -> c.getCreatedAt() != null && !c.getCreatedAt().isBefore(range[0]) && c.getCreatedAt().isBefore(range[1])).count();
                    long activeCust = entry.getValue().stream().map(Invoice::getIssuedToCustomerId).distinct().count();
                    return new AnalyticsDTO.MonthlyGrowthDTO(label, (int)newCust, (int)activeCust);
                })
                .collect(Collectors.toList());
    }

    private List<AnalyticsDTO.ServiceBreakdownDTO> getServiceMix(List<Booking> bookings) {
        return bookings.stream()
                .filter(b -> b.getPackageId() != null)
                .collect(Collectors.groupingBy(Booking::getPackageId, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> {
                    String name = servicePackageRepository.findById(entry.getKey()).map(ServicePackage::getName).orElse("General Service");
                    return new AnalyticsDTO.ServiceBreakdownDTO(name, entry.getValue().intValue());
                })
                .collect(Collectors.toList());
    }

    private List<AnalyticsDTO.CenterPerformanceDTO> getCenterRankings(Set<UUID> ids, List<Invoice> invoices, List<Booking> bookings) {
        return ids.stream()
                .map(id -> {
                    ServiceCenter center = serviceCenterRepository.findById(id).orElse(null);
                    if (center == null) return null;
                    long jobs = bookings.stream().filter(b -> b.getCenterId().equals(id)).count();
                    BigDecimal rev = invoices.stream().filter(i -> i.getCenterId().equals(id) && "PAID".equalsIgnoreCase(i.getStatus())).map(Invoice::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new AnalyticsDTO.CenterPerformanceDTO(id.toString(), center.getName(), center.getName().substring(0, 1).toUpperCase(), "#EA580C", (int) jobs, rev);
                })
                .filter(Objects::nonNull)
                .sorted((c1, c2) -> c2.getRevenue().compareTo(c1.getRevenue()))
                .collect(Collectors.toList());
    }

    private int getTimeKey(LocalDateTime dt, String period) {
        if ("daily".equalsIgnoreCase(period)) return dt.getYear() * 10000 + dt.getMonthValue() * 100 + dt.getDayOfMonth();
        if ("yearly".equalsIgnoreCase(period)) return dt.getYear();
        return dt.getYear() * 100 + dt.getMonthValue();
    }

    private String formatLabel(int key, String period) {
        if ("daily".equalsIgnoreCase(period)) return (key % 100) + " " + java.time.Month.of((key % 10000) / 100).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        if ("yearly".equalsIgnoreCase(period)) return String.valueOf(key);
        return java.time.Month.of(key % 100).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }

    private LocalDateTime[] getRange(int key, String period) {
        if ("daily".equalsIgnoreCase(period)) {
            LocalDateTime s = LocalDateTime.of(key / 10000, (key % 10000) / 100, key % 100, 0, 0);
            return new LocalDateTime[]{s, s.plusDays(1)};
        }
        if ("yearly".equalsIgnoreCase(period)) {
            LocalDateTime s = LocalDateTime.of(key, 1, 1, 0, 0);
            return new LocalDateTime[]{s, s.plusYears(1)};
        }
        LocalDateTime s = LocalDateTime.of(key / 100, key % 100, 1, 0, 0);
        return new LocalDateTime[]{s, s.plusMonths(1)};
    }

    private String calculatePercentageChange(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? "+100%" : ZERO_PERCENT;
        }
        BigDecimal change = current.subtract(previous).divide(previous, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
        return (change.compareTo(BigDecimal.ZERO) >= 0 ? POSITIVE_PREFIX : "") + change.toString() + PERCENT_SUFFIX;
    }
}
