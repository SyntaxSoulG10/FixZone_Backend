package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.InitPaymentRequest;
import com.fixzone.fixzon_backend.enums.BookingStatus;
import com.fixzone.fixzon_backend.enums.PaymentStatus;
import com.fixzone.fixzon_backend.model.Booking;
import com.fixzone.fixzon_backend.model.Payment;
import com.fixzone.fixzon_backend.model.ServicePackage;
import com.fixzone.fixzon_backend.repository.BookingRepository;
import com.fixzone.fixzon_backend.repository.PaymentRepository;
import com.fixzone.fixzon_backend.repository.ServicePackageRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    @Value("${stripe.secret-key}")
    private String stripeApiKey;

    private final PaymentRepository paymentRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final BookingRepository bookingRepository;

    public PaymentService(PaymentRepository paymentRepository,
                          ServicePackageRepository servicePackageRepository,
                          BookingRepository bookingRepository) {
        this.paymentRepository = paymentRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.bookingRepository = bookingRepository;
    }

    public Payment initPayment(InitPaymentRequest request, String bookingId) {
        if (request.getServicePackageId() == null) throw new RuntimeException("Service Package ID is missing");
        
        UUID packageId = UUID.fromString(request.getServicePackageId());
        ServicePackage servicePackage = servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new RuntimeException("Service package not found"));

        double totalAmount = servicePackage.getBasePrice().doubleValue();
        double initialAmount = totalAmount * 0.4; // 40% initial payment

        Payment payment = new Payment();
        if (bookingId != null && !bookingId.isEmpty()) {
            try {
                payment.setBookingId(Long.parseLong(bookingId));
            } catch (NumberFormatException e) {
                // If it's a UUID string, it will be handled by the Detail Matcher during session creation
            }
        }
        payment.setServicePackageId(packageId);
        payment.setVehicleId(UUID.fromString(request.getVehicleId()));
        payment.setCenterId(UUID.fromString(request.getCenterId()));
        payment.setDate(request.getDate());
        payment.setTimeSlot(request.getTimeSlot());
        payment.setAmount(initialAmount);
        payment.setStatus(PaymentStatus.PENDING);

        return paymentRepository.save(payment);
    }

    public String createStripeSession(Long paymentId) throws StripeException {
        Stripe.apiKey = stripeApiKey;

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment record not found"));

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("http://localhost:3000/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl("http://localhost:3000/dashboard/customer/checkout")
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("lkr")
                                .setUnitAmount((long) (payment.getAmount() * 100))
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Service Booking Fee")
                                        .build())
                                .build())
                        .build())
                .build();

        try {
            Session session = Session.create(params);
            payment.setGatewaySessionId(session.getId());
            paymentRepository.save(payment);

            // Robust Search: Find the booking by ID or by matching service details
            Optional<Booking> bookingOpt = bookingRepository.findAll().stream()
                    .filter(b -> {
                        if (b.getBookingId() == null) return false;
                        
                        // 1. Try matching by Booking ID
                        String bId = b.getBookingId().toString();
                        String pId = payment.getBookingId() != null ? payment.getBookingId().toString() : "";
                        if (bId.equalsIgnoreCase(pId)) return true;

                        // 2. Fallback: Match by Service Package + Vehicle + Date
                        boolean packageMatch = b.getPackageId() != null && b.getPackageId().equals(payment.getServicePackageId());
                        boolean vehicleMatch = b.getVehicleId() != null && b.getVehicleId().equals(payment.getVehicleId());
                        boolean dateMatch = b.getBookingDate() != null && b.getBookingDate().toString().equals(payment.getDate());
                        
                        return packageMatch && vehicleMatch && dateMatch;
                    })
                    .findFirst();

            if (bookingOpt.isPresent()) {
                Booking booking = bookingOpt.get();
                System.out.println(">>> LINKING STRIPE SESSION TO BOOKING: " + booking.getBookingId());
                booking.setGatewaySessionId(session.getId());
                booking.setBookingFee(BigDecimal.valueOf(payment.getAmount()));
                bookingRepository.save(booking);
            }
            return session.getUrl();
        } catch (StripeException e) {
            System.err.println("STRIPE ERROR: " + e.getMessage());
            throw e;
        }
    }

    @Transactional
    public boolean handleSuccess(String sessionId) {
        Stripe.apiKey = stripeApiKey;
        try {
            Session session = Session.retrieve(sessionId);
            if ("paid".equalsIgnoreCase(session.getPaymentStatus())) {
                Optional<Payment> paymentOpt = paymentRepository.findAll().stream()
                        .filter(p -> sessionId.equals(p.getGatewaySessionId()))
                        .findFirst();

                if (paymentOpt.isPresent()) {
                    Payment payment = paymentOpt.get();
                    payment.setStatus(PaymentStatus.PAID); // Fixed: Changed SUCCESSFUL to PAID
                    paymentRepository.save(payment);

                    // Update Booking (Find existing or create new)
                    Optional<Booking> bookingOpt = bookingRepository.findAll().stream()
                            .filter(b -> sessionId.equals(b.getGatewaySessionId()))
                            .findFirst();

                    Booking booking;
                    if (bookingOpt.isPresent()) {
                        booking = bookingOpt.get();
                    } else {
                        // Create NEW Booking from Payment details
                        booking = new Booking();
                        booking.setBookingId(UUID.randomUUID());
                        
                        // MOCK CUSTOMER ID for UI Testing
                        booking.setCustomerId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
                        
                        booking.setTenantId(payment.getTenantId());
                        booking.setCenterId(payment.getCenterId());
                        booking.setVehicleId(payment.getVehicleId());
                        booking.setPackageId(payment.getServicePackageId());
                        booking.setBookingDate(java.time.LocalDate.parse(payment.getDate()));
                        
                        // Fix: Parse only the START time from a range like "18:00-19:00"
                        String timeStr = payment.getTimeSlot();
                        if (timeStr != null && timeStr.contains("-")) {
                            timeStr = timeStr.split("-")[0].trim();
                        }
                        booking.setBookingTime(java.time.LocalTime.parse(timeStr));
                        booking.setGatewaySessionId(sessionId);
                        booking.setBookingFee(BigDecimal.valueOf(payment.getAmount()));
                        booking.setCenterId(payment.getCenterId());
                        booking.setTenantId(payment.getTenantId() != null ? payment.getTenantId() : UUID.randomUUID());
                    }
                    
                    booking.setStatus(BookingStatus.CONFIRMED);
                    booking.setBookingFeePaid(true);
                    bookingRepository.save(booking);
                    return true;
                }
            }
        } catch (StripeException e) {
            System.err.println("SUCCESS VERIFICATION ERROR: " + e.getMessage());
        }
        return false;
    }

    public boolean refundPayment(String gatewaySessionId, double penaltyPercentage) {
        Stripe.apiKey = stripeApiKey;
        try {
            Session session = Session.retrieve(gatewaySessionId);
            String paymentIntentId = session.getPaymentIntent();

            long refundAmount = session.getAmountTotal();
            if (penaltyPercentage > 0) {
                refundAmount = (long) (refundAmount * (1 - (penaltyPercentage / 100)));
            }

            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntentId)
                    .setAmount(refundAmount)
                    .build();

            Refund.create(params);
            return true;
        } catch (StripeException e) {
            System.err.println("REFUND ERROR: " + e.getMessage());
            return false;
        }
    }

    public Payment getPaymentStatus(Long bookingId) {
        return paymentRepository.findAll().stream()
                .filter(p -> p.getBookingId() != null && p.getBookingId().equals(bookingId))
                .findFirst()
                .orElse(null);
    }

    public String reschedulePayment(Long bookingId) throws StripeException {
        Payment payment = getPaymentStatus(bookingId);
        if (payment == null) {
            throw new RuntimeException("Payment not found for booking");
        }
        return createStripeSession(payment.getId());
    }
}
