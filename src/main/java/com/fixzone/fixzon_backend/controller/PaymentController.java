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
        com.fixzone.fixzon_backend.model.Payment payment = paymentService.initPayment(request, request.getBookingId(), customerEmail);
        java.util.Map<String, Object> eligibility = paymentService.validatePayoutEligibility(payment.getTenantId());
        return ResponseEntity.ok(java.util.Map.of(
                "paymentId", payment.getId(),
                "stripeConnected", eligibility.get("stripeConnected"),
                "message", eligibility.get("message")
        ));
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

    @PostMapping("/stripe/mobile-sheet")
    public ResponseEntity<?> createMobilePaymentSheet(@RequestBody java.util.Map<String, Long> payload) {
        Long paymentId = payload.get("paymentId");
        if (paymentId == null) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Payment ID is required"));
        }
        try {
            log.info(">>> HIT STRIPE MOBILE SHEET FOR PAYMENT ID: {}", paymentId);
            java.util.Map<String, Object> result = paymentService.createStripePaymentIntent(paymentId);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of(
                    "error", ex.getMessage(),
                    "requiresStripeConnect", true
            ));
        } catch (StripeException ex) {
            log.error("Stripe payment intent creation failed", ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(java.util.Map.of(
                    "error", "Stripe payment intent could not be created right now.",
                    "details", ex.getMessage()
            ));
        }
    }

    @GetMapping("/payment-status/{paymentId}")
    public ResponseEntity<?> getPaymentStatusById(@PathVariable Long paymentId) {
        com.fixzone.fixzon_backend.enums.PaymentStatus status = paymentService.getPaymentStatusById(paymentId);
        if (status != null) {
            return ResponseEntity.ok(java.util.Map.of("status", status.name()));
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            paymentService.handleStripeWebhook(payload, sigHeader);
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            log.error("Webhook error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook handling failed");
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
