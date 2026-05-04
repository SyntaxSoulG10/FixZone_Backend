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
import com.fixzone.fixzon_backend.repository.AuthRepository;
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

    // Stripe API key injected from application properties for payment processing
    @Value("${stripe.secret-key}")
    private String stripeApiKey;

    // Repository for payment record operations
    private final PaymentRepository paymentRepository;
    
    // Repository for service package information (pricing, details)
    private final ServicePackageRepository servicePackageRepository;
    
    // Repository for booking operations
    private final BookingRepository bookingRepository;
    
    // Repository for user/authentication information
    private final AuthRepository authRepository;

    // Constructor-based dependency injection
    public PaymentService(PaymentRepository paymentRepository,
                          ServicePackageRepository servicePackageRepository,
                          BookingRepository bookingRepository,
                          AuthRepository authRepository) {
        this.paymentRepository = paymentRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.bookingRepository = bookingRepository;
        this.authRepository = authRepository;
    }

    // Initializes payment with 40% initial payment at booking time
    public Payment initPayment(InitPaymentRequest request, String bookingId, String customerEmail) {
        if (request.getServicePackageId() == null) throw new RuntimeException("Service Package ID is missing");
        
        UUID packageId = UUID.fromString(request.getServicePackageId());
        ServicePackage servicePackage = servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new RuntimeException("Service package not found"));

        double totalAmount = servicePackage.getBasePrice().doubleValue();
        double initialAmount = totalAmount * 0.4;

        Payment payment = new Payment();
        if (bookingId != null && !bookingId.isEmpty()) {
            try {
                payment.setBookingId(Long.parseLong(bookingId));
            } catch (NumberFormatException e) {
                // If bookingId is UUID string, it will be matched during session creation
            }
        }
        payment.setServicePackageId(packageId);
        payment.setVehicleId(UUID.fromString(request.getVehicleId()));
        payment.setCenterId(UUID.fromString(request.getCenterId()));
        payment.setDate(request.getDate());
        payment.setTimeSlot(request.getTimeSlot());
        payment.setAmount(initialAmount);
        payment.setStatus(PaymentStatus.PENDING);

        if (customerEmail != null) {
            authRepository.findByEmail(customerEmail).ifPresent(user -> {
                payment.setCustomerId(user.getUserId());
            });
        }

        return paymentRepository.save(payment);
    }

    // Creates Stripe checkout session (returns payment URL)
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

            Optional<Booking> bookingOpt = bookingRepository.findAll().stream()
                    .filter(b -> {
                        if (b.getBookingId() == null) return false;
                        String bId = b.getBookingId().toString();
                        String pId = payment.getBookingId() != null ? payment.getBookingId().toString() : "";
                        if (bId.equalsIgnoreCase(pId)) return true;
                        boolean packageMatch = b.getPackageId() != null && b.getPackageId().equals(payment.getServicePackageId());
                        boolean vehicleMatch = b.getVehicleId() != null && b.getVehicleId().equals(payment.getVehicleId());
                        boolean dateMatch = b.getBookingDate() != null && b.getBookingDate().toString().equals(payment.getDate());
                        return packageMatch && vehicleMatch && dateMatch;
                    })
                    .findFirst();

            if (bookingOpt.isPresent()) {
                Booking booking = bookingOpt.get();
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

    // Handles successful payment - updates payment and booking to CONFIRMED
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
                    payment.setStatus(PaymentStatus.PAID);
                    paymentRepository.save(payment);

                    Optional<Booking> bookingOpt = bookingRepository.findAll().stream()
                            .filter(b -> sessionId.equals(b.getGatewaySessionId()))
                            .findFirst();

                    Booking booking;
                    if (bookingOpt.isPresent()) {
                        booking = bookingOpt.get();
                    } else {
                        booking = new Booking();
                        booking.setBookingId(UUID.randomUUID());
                        if (payment.getCustomerId() != null) {
                            booking.setCustomerId(payment.getCustomerId());
                        } else {
                            throw new RuntimeException("Cannot create booking: Customer ID is missing from payment record");
                        }
                        booking.setTenantId(payment.getTenantId());
                        booking.setCenterId(payment.getCenterId());
                        booking.setVehicleId(payment.getVehicleId());
                        booking.setPackageId(payment.getServicePackageId());
                        booking.setBookingDate(java.time.LocalDate.parse(payment.getDate()));
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

    // Processes refund with penalty deduction if within cancellation window
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

    // Retrieves payment status for a booking
    public Payment getPaymentStatus(Long bookingId) {
        return paymentRepository.findAll().stream()
                .filter(p -> p.getBookingId() != null && p.getBookingId().equals(bookingId))
                .findFirst()
                .orElse(null);
    }

    // Re-initiates payment processing for rescheduled booking
    public String reschedulePayment(Long bookingId) throws StripeException {
        Payment payment = getPaymentStatus(bookingId);
        if (payment == null) {
            throw new RuntimeException("Payment not found for booking");
        }
        return createStripeSession(payment.getId());
    }
}
