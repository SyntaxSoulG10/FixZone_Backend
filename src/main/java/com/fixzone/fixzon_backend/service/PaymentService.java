package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.enums.PaymentStatus;
import com.fixzone.fixzon_backend.model.Payment;
import com.fixzone.fixzon_backend.repository.PaymentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final com.fixzone.fixzon_backend.repository.ServicePackageRepository servicePackageRepository;

    public PaymentService(PaymentRepository paymentRepository, 
                          com.fixzone.fixzon_backend.repository.ServicePackageRepository servicePackageRepository) {
        this.paymentRepository = paymentRepository;
        this.servicePackageRepository = servicePackageRepository;
    }

    public com.fixzone.fixzon_backend.DTO.InitPaymentResponse initPayment(com.fixzone.fixzon_backend.DTO.InitPaymentRequest request) {
        // Mock Slot Availability - Always valid for now
        
        // Fetch Service Package
        com.fixzone.fixzon_backend.model.ServicePackage pkg = servicePackageRepository.findById(Objects.requireNonNull(request.getServicePackageId()))
                .orElseThrow(() -> new IllegalArgumentException("Service Package not found"));

        // Save PENDING Payment
        Payment payment = new Payment();
        payment.setBookingId(101L); // Mock bookingId
        payment.setServicePackageId(request.getServicePackageId());
        payment.setVehicleId(request.getVehicleId());
        payment.setDate(request.getDate());
        payment.setTimeSlot(request.getTimeSlot());
        payment.setAmount(pkg.getBasePrice().doubleValue());
        payment.setStatus(PaymentStatus.PENDING);
        
        Payment saved = paymentRepository.save(payment);
        return new com.fixzone.fixzon_backend.DTO.InitPaymentResponse(saved.getId(), saved.getAmount());
    }

    public String createStripeSession(Long paymentId) throws StripeException {
        Payment payment = paymentRepository.findById(Objects.requireNonNull(paymentId))
                .orElseThrow(() -> new IllegalArgumentException("Payment record not found"));

        // Fetch package details for the Stripe session name
        String productName = "Booking Payment";
        if (payment.getServicePackageId() != null) {
            productName = servicePackageRepository.findById(Objects.requireNonNull(payment.getServicePackageId()))
                    .map(com.fixzone.fixzon_backend.model.ServicePackage::getName)
                    .orElse("Booking Payment");
        }

        long amountInCents = (long) (payment.getAmount() * 100);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("http://localhost:3000/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl("http://localhost:3000/cancel")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("lkr")
                                                .setUnitAmount(amountInCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(productName)
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        Session session = Session.create(params);
        payment.setStripeSessionId(session.getId());
        paymentRepository.save(payment);
        return session.getUrl();
    }

    public boolean handleSuccess(String sessionId) {
        Optional<Payment> optionalPayment = paymentRepository.findByStripeSessionId(sessionId);
        if (optionalPayment.isPresent()) {
            Payment payment = optionalPayment.get();
            try {
                // Verify with Stripe
                Session stripeSession = Session.retrieve(sessionId);
                if ("paid".equalsIgnoreCase(stripeSession.getPaymentStatus())) {
                    payment.setStatus(PaymentStatus.PAID);
                    paymentRepository.save(payment);
                    return true;
                }
            } catch (StripeException e) {
                // Log exception
                e.printStackTrace();
            }
        }
        return false;
    }

    public Payment getPaymentStatus(Long bookingId) {
        return paymentRepository.findFirstByBookingIdOrderByIdDesc(bookingId).orElse(null);
    }

    public boolean refundPayment(Long bookingId) {
        Optional<Payment> optionalPayment = paymentRepository.findFirstByBookingIdOrderByIdDesc(bookingId);
        if (optionalPayment.isPresent()) {
            Payment payment = optionalPayment.get();
            if (payment.getStatus() == PaymentStatus.PAID) {
                try {
                    // Calculate 50% refund
                    double refundAmount = payment.getAmount() * 0.5;
                    long refundAmountInCents = (long) (refundAmount * 100);

                    // Retrieve session to get PaymentIntent id for refund
                    Session stripeSession = Session.retrieve(payment.getStripeSessionId());
                    String paymentIntentId = stripeSession.getPaymentIntent();

                    if (paymentIntentId != null) {
                        RefundCreateParams refundParams = RefundCreateParams.builder()
                                .setPaymentIntent(paymentIntentId)
                                .setAmount(refundAmountInCents)
                                .build();
                        
                        Refund.create(refundParams);

                        // Update local status
                        payment.setStatus(PaymentStatus.REFUNDED);
                        paymentRepository.save(payment);
                        return true;
                    }
                } catch (StripeException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    public String reschedulePayment(Long bookingId) throws StripeException {
        Optional<Payment> optionalPayment = paymentRepository.findFirstByBookingIdOrderByIdDesc(bookingId);
        if (optionalPayment.isPresent()) {
            Payment originalPayment = optionalPayment.get();
            double extraAmount = originalPayment.getAmount() * 0.3;
            long amountInCents = (long) (extraAmount * 100);

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("http://localhost:3000/success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl("http://localhost:3000/cancel")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("lkr")
                                                    .setUnitAmount(amountInCents)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Reschedule Payment (30%)")
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);

            // Save new payment record for the 30% reschedule fee
            Payment newPayment = new Payment();
            newPayment.setBookingId(bookingId);
            newPayment.setAmount(extraAmount);
            newPayment.setStripeSessionId(session.getId());
            newPayment.setStatus(PaymentStatus.PENDING);
            paymentRepository.save(newPayment);

            return session.getUrl();
        }
        throw new IllegalArgumentException("Original payment not found for bookingId: " + bookingId);
    }
}
