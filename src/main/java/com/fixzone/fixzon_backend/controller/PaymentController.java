package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.InitPaymentRequest;
import com.fixzone.fixzon_backend.DTO.RefundRequest;
import com.fixzone.fixzon_backend.DTO.RescheduleRequest;
import com.fixzone.fixzon_backend.model.Payment;
import com.fixzone.fixzon_backend.service.PaymentService;
import java.util.UUID;
import com.stripe.exception.StripeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.net.URI;


import org.springframework.beans.factory.annotation.Value;


@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/init")
    public ResponseEntity<?> initPayment(@RequestBody InitPaymentRequest request, java.security.Principal principal) {
        String customerEmail = principal != null ? principal.getName() : null;
        try {
            com.fixzone.fixzon_backend.model.Payment payment = paymentService.initPayment(request, request.getBookingId(), customerEmail);
            return ResponseEntity.ok(java.util.Map.of(
                    "paymentId", payment.getId(),
                    "stripeConnected", true,
                    "message", "Payment initialised successfully."
            ));
        } catch (IllegalStateException ex) {
            // Owner has not completed Stripe Connect — return a clear 409 so the UI
            // can display "This branch cannot accept online payments yet" immediately.
            log.warn(">>> PAYMENT INIT BLOCKED: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of(
                    "stripeConnected", false,
                    "requiresStripeConnect", true,
                    "message", ex.getMessage()
            ));
        }
    }

    @PostMapping("/stripe")
    public ResponseEntity<?> createSession(@RequestBody java.util.Map<String, Long> payload) {
        Long paymentId = payload.get("paymentId");
        if (paymentId == null) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Payment ID is required"));
        }

        try {
            log.info(">>> HIT STRIPE SESSION FOR PAYMENT ID: {}", paymentId);
            String sessionUrl = paymentService.createStripeSession(paymentId);
            return ResponseEntity.ok(java.util.Map.of("checkoutUrl", sessionUrl));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of(
                    "error", ex.getMessage(),
                    "requiresStripeConnect", true
            ));
        } catch (StripeException ex) {
            log.error("Stripe session creation failed", ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(java.util.Map.of(
                    "error", "Stripe checkout could not be created right now.",
                    "details", ex.getMessage()
            ));
        }
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

    /**
     * Returns the internal payment record ID for a given booking UUID.
     * Used by the frontend "Proceed to Payment" button for PENDING_PAYMENT bookings.
     */
    @GetMapping("/by-booking/{bookingId}")
    public ResponseEntity<?> getPaymentByBookingUUID(@PathVariable UUID bookingId) {
        try {
            Long paymentId = paymentService.findPaymentIdByBookingUUID(bookingId);
            if (paymentId == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(java.util.Map.of("paymentId", paymentId));
        } catch (Exception e) {
            log.error("Error finding payment for booking {}: {}", bookingId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Could not find payment record"));
        }
    }

    @PostMapping("/connect")
    public ResponseEntity<String> generateConnectLink(java.security.Principal principal, HttpServletRequest request) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User must be logged in");
        }
        try {
            String link = paymentService.generateConnectLink(principal.getName(), request);
            return ResponseEntity.ok(link);
        } catch (IllegalStateException e) {
            log.error("Stripe Connect Error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Failed to generate connect link: " + e.getMessage());
        } catch (StripeException e) {
            log.error("Stripe Connect Error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to generate connect link: " + e.getMessage());
        }
    }

    /**
     * Returns the current owner's Stripe Connect status.
     * Used by the branch creation form to show/hide the Stripe Connect step.
     */
    @GetMapping("/connect/status")
    public ResponseEntity<?> getConnectStatus(java.security.Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User must be logged in");
        }
        return paymentService.getConnectStatus(principal.getName());
    }

    @GetMapping("/connect/callback")
    public ResponseEntity<Void> handleConnectCallback(@RequestParam("accountId") String accountId) {
        try {
            paymentService.handleConnectCallback(accountId);
            // Redirect back to branch creation page with success flag
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/dashboard/company-owner/centers?connect=success"))
                    .build();
        } catch (Exception e) {
            log.error("Stripe Connect Callback Error: ", e);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/dashboard/company-owner/centers?connect=error"))
                    .build();
        }
    }
}
