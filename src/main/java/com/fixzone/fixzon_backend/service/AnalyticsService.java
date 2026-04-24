package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.AnalyticsDTO;
import com.fixzone.fixzon_backend.enums.BookingStatus;
import com.fixzone.fixzon_backend.model.Booking;
import com.fixzone.fixzon_backend.model.Invoice;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.repository.BookingRepository;
import com.fixzone.fixzon_backend.repository.InvoiceRepository;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.CustomerRepository;
import com.fixzone.fixzon_backend.repository.PaymentRecordRepository;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
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
    private final CustomerRepository customerRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final OwnerRepository ownerRepository;

    public AnalyticsService(InvoiceRepository invoiceRepository,
                            BookingRepository bookingRepository,
                            ServiceCenterRepository serviceCenterRepository,
                            CustomerRepository customerRepository,
                            PaymentRecordRepository paymentRecordRepository,
                            OwnerRepository ownerRepository) {
        this.invoiceRepository = invoiceRepository;
        this.bookingRepository = bookingRepository;
        this.serviceCenterRepository = serviceCenterRepository;
        this.customerRepository = customerRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.ownerRepository = ownerRepository;
    }

    public AnalyticsDTO getCompanyAnalytics(String companyCode, String centerIdStr, String startDateStr, String endDateStr, String period) {
        UUID filterCenterId = (centerIdStr != null && !centerIdStr.equals("all") && !centerIdStr.isEmpty())
                ? UUID.fromString(centerIdStr)
                : null;
        LocalDateTime startFilter = (startDateStr != null && !startDateStr.isEmpty())
                ? java.time.LocalDate.parse(startDateStr).atStartOfDay()
                : null;
        LocalDateTime endFilter = (endDateStr != null && !endDateStr.isEmpty())
                ? java.time.LocalDate.parse(endDateStr).atTime(23, 59, 59)
                : null;

        List<Invoice> allInvoices = invoiceRepository.findByCompanyCode(companyCode);

        Set<UUID> centerIds;
        if (filterCenterId != null) {
            centerIds = Set.of(filterCenterId);
        } else {
            centerIds = ownerRepository.findByOwnerCode(companyCode)
                    .map(owner -> serviceCenterRepository.findByOwner_UserId(owner.getUserId()))
                    .map(list -> list.stream().map(ServiceCenter::getCenterId).collect(Collectors.toSet()))
                    .orElseGet(() -> allInvoices.stream().map(Invoice::getCenterId).collect(Collectors.toSet()));
        }

        List<Invoice> invoices = allInvoices.stream()
                .filter(i -> filterCenterId == null || i.getCenterId().equals(filterCenterId))
                .filter(i -> startFilter == null || i.getIssuedAt().isAfter(startFilter) || i.getIssuedAt().isEqual(startFilter))
                .filter(i -> endFilter == null || i.getIssuedAt().isBefore(endFilter) || i.getIssuedAt().isEqual(endFilter))
                .collect(Collectors.toList());

        BigDecimal totalRevenue = invoices.stream()
                .filter(i -> "PAID".equalsIgnoreCase(i.getStatus()))
                .map(Invoice::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Booking> allBookings = bookingRepository.findByCenterIdIn(centerIds);
        List<Booking> bookings = allBookings.stream()
                .filter(b -> startFilter == null || b.getBookingDate().isAfter(startFilter.toLocalDate()) || b.getBookingDate().isEqual(startFilter.toLocalDate()))
                .filter(b -> endFilter == null || b.getBookingDate().isBefore(endFilter.toLocalDate()) || b.getBookingDate().isEqual(endFilter.toLocalDate()))
                .collect(Collectors.toList());

        long totalJobs = bookings.size();
        long pendingJobs = bookings.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
        BigDecimal avgJobValue = totalJobs > 0 ? totalRevenue.divide(BigDecimal.valueOf(totalJobs), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        // --- TREND CALCULATIONS ---
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime firstDayCurrentMonth = now.withDayOfMonth(1).withHour(0).withMinute(0);
        LocalDateTime firstDayLastMonth = firstDayCurrentMonth.minusMonths(1);

        BigDecimal currentMonthRevenue = allInvoices.stream()
                .filter(i -> "PAID".equalsIgnoreCase(i.getStatus()) && !i.getIssuedAt().isBefore(firstDayCurrentMonth))
                .map(Invoice::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal lastMonthRevenue = allInvoices.stream()
                .filter(i -> "PAID".equalsIgnoreCase(i.getStatus()) && !i.getIssuedAt().isBefore(firstDayLastMonth) && i.getIssuedAt().isBefore(firstDayCurrentMonth))
                .map(Invoice::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        String revenueChange = calculatePercentageChange(currentMonthRevenue, lastMonthRevenue);

        long currentMonthJobs = allBookings.stream().filter(b -> b.getBookingDate() != null && !b.getBookingDate().isBefore(firstDayCurrentMonth.toLocalDate())).count();
        long lastMonthJobs = allBookings.stream().filter(b -> b.getBookingDate() != null && !b.getBookingDate().isBefore(firstDayLastMonth.toLocalDate()) && b.getBookingDate().isBefore(firstDayCurrentMonth.toLocalDate())).count();
        String jobsChange = calculatePercentageChange(BigDecimal.valueOf(currentMonthJobs), BigDecimal.valueOf(lastMonthJobs));

        BigDecimal currentAvg = currentMonthJobs > 0 ? currentMonthRevenue.divide(BigDecimal.valueOf(currentMonthJobs), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal lastAvg = lastMonthJobs > 0 ? lastMonthRevenue.divide(BigDecimal.valueOf(lastMonthJobs), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        String avgJobValueChange = calculatePercentageChange(currentAvg, lastAvg);

        long pendingOld = allBookings.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED && b.getBookingDate() != null && b.getBookingDate().isBefore(now.toLocalDate().minusDays(7))).count();
        String pendingJobsChange = calculatePercentageChange(BigDecimal.valueOf(pendingJobs), BigDecimal.valueOf(pendingOld));

        // --- CHART DATA ---
        List<com.fixzone.fixzon_backend.model.PaymentRecord> allPayments = centerIds.stream()
                .flatMap(centerId -> paymentRecordRepository.findByCenterId(centerId).stream()).collect(Collectors.toList());

        BigDecimal onlineRevenue = allPayments.stream()
                .filter(p -> "CARD".equalsIgnoreCase(p.getMethod()) || "ONLINE".equalsIgnoreCase(p.getMethod()))
                .map(com.fixzone.fixzon_backend.model.PaymentRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal handCollectionRevenue = allPayments.stream()
                .filter(p -> "CASH".equalsIgnoreCase(p.getMethod()))
                .map(com.fixzone.fixzon_backend.model.PaymentRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<AnalyticsDTO.MonthlyDataDTO> revenueOverview = invoices.stream()
                .filter(i -> "PAID".equalsIgnoreCase(i.getStatus()))
                .collect(Collectors.groupingBy(i -> {
                    if ("daily".equalsIgnoreCase(period)) return i.getIssuedAt().getYear() * 10000 + i.getIssuedAt().getMonthValue() * 100 + i.getIssuedAt().getDayOfMonth();
                    return i.getIssuedAt().getYear() * 100 + i.getIssuedAt().getMonthValue();
                }, TreeMap::new, Collectors.toList()))
                .entrySet().stream().map(entry -> {
                    String name = "Data"; // Simplified for merge stability
                    return new AnalyticsDTO.MonthlyDataDTO(name, entry.getValue().stream().map(Invoice::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add), BigDecimal.ZERO, BigDecimal.ZERO);
                }).collect(Collectors.toList());

        List<AnalyticsDTO.CenterPerformanceDTO> topCenters = centerIds.stream().map(centerId -> {
            ServiceCenter center = serviceCenterRepository.findById(centerId).orElse(null);
            if (center == null) return null;
            BigDecimal rev = invoices.stream().filter(i -> i.getCenterId().equals(centerId) && "PAID".equalsIgnoreCase(i.getStatus())).map(Invoice::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            return new AnalyticsDTO.CenterPerformanceDTO(centerId.toString(), center.getName(), center.getName().substring(0, 1), "#EA580C", 0, rev);
        }).filter(Objects::nonNull).collect(Collectors.toList());

        String updatedAt = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"));

        return new AnalyticsDTO(totalRevenue, revenueChange, totalJobs, jobsChange, pendingJobs, pendingJobsChange,
                avgJobValue, avgJobValueChange, updatedAt, onlineRevenue, handCollectionRevenue,
                revenueOverview, new ArrayList<>(), new ArrayList<>(), topCenters);
    }

    private String calculatePercentageChange(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) return current.compareTo(BigDecimal.ZERO) > 0 ? "+100%" : "0.0%";
        BigDecimal change = current.subtract(previous).divide(previous, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
        return (change.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + change.toString() + "%";
    }
}
