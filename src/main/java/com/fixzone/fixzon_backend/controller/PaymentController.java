package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.InitPaymentRequest;
import com.fixzone.fixzon_backend.DTO.RefundRequest;
import com.fixzone.fixzon_backend.DTO.RescheduleRequest;
import com.fixzone.fixzon_backend.model.Payment;
import com.fixzone.fixzon_backend.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/payments")
@CrossOrigin(origins = "http://localhost:3000")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/init")
    public ResponseEntity<?> initPayment(@RequestBody InitPaymentRequest request) {
        try {
            com.fixzone.fixzon_backend.model.Payment payment = paymentService.initPayment(request, request.getBookingId());
            return ResponseEntity.ok(java.util.Map.of("paymentId", payment.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/stripe")
    public ResponseEntity<String> createSession(@RequestBody java.util.Map<String, Long> payload) {
        Long paymentId = payload.get("paymentId");
        System.out.println(">>> HIT STRIPE SESSION FOR PAYMENT ID: " + paymentId);
        try {
            String sessionUrl = paymentService.createStripeSession(paymentId);
            return ResponseEntity.ok(sessionUrl);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
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
    public ResponseEntity<String> reschedule(@RequestBody RescheduleRequest rescheduleRequest) {
        try {
            String newSessionUrl = paymentService.reschedulePayment(rescheduleRequest.getBookingId());
            return ResponseEntity.ok(newSessionUrl);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
