package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.InitPaymentRequest;
import com.fixzone.fixzon_backend.DTO.RefundRequest;
import com.fixzone.fixzon_backend.DTO.RescheduleRequest;
import com.fixzone.fixzon_backend.model.Payment;
import com.fixzone.fixzon_backend.service.PaymentService;
import com.stripe.exception.StripeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/payments")
public class PaymentController {
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/init")
    public ResponseEntity<?> initPayment(@RequestBody InitPaymentRequest request, java.security.Principal principal) {
        // If authenticated, we should use the real customer ID
        String customerEmail = principal != null ? principal.getName() : null;
        com.fixzone.fixzon_backend.model.Payment payment = paymentService.initPayment(request, request.getBookingId(), customerEmail);
        return ResponseEntity.ok(java.util.Map.of("paymentId", payment.getId()));
    }

    @PostMapping("/stripe")
    public ResponseEntity<String> createSession(@RequestBody java.util.Map<String, Long> payload) throws StripeException {
        Long paymentId = payload.get("paymentId");
        log.info(">>> HIT STRIPE SESSION FOR PAYMENT ID: {}", paymentId);
        String sessionUrl = paymentService.createStripeSession(paymentId);
        return ResponseEntity.ok(sessionUrl);
    }

    @GetMapping("/success")
    public ResponseEntity<String> paymentSuccess(@RequestParam("session_id") String sessionId) {
        boolean success = paymentService.handleSuccess(sessionId);
        if (success) {
            return ResponseEntity.ok("Payment successful and booking confirmed!");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment verification failed.");
        }
    }

    @GetMapping("/status/{bookingId}")
    public ResponseEntity<Payment> getPaymentStatus(@PathVariable Long bookingId) {
        Payment payment = paymentService.getPaymentStatus(bookingId);
        if (payment != null) {
            return ResponseEntity.ok(payment);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/refund")
    public ResponseEntity<String> refundPayment(@RequestBody RefundRequest refundRequest) {
        // Find the payment record by bookingId to get the gateway session ID
        Payment payment = paymentService.getPaymentStatus(refundRequest.getBookingId());
        if (payment != null && payment.getGatewaySessionId() != null) {
            boolean success = paymentService.refundPayment(payment.getGatewaySessionId(), refundRequest.getPenaltyPercentage());
            if (success) {
                return ResponseEntity.ok("Refund processed successfully.");
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Refund failed.");
    }

    @PostMapping("/reschedule")
    public ResponseEntity<String> reschedule(@RequestBody RescheduleRequest rescheduleRequest) throws StripeException {
        String newSessionUrl = paymentService.reschedulePayment(rescheduleRequest.getBookingId());
        return ResponseEntity.ok(newSessionUrl);
    }
}
