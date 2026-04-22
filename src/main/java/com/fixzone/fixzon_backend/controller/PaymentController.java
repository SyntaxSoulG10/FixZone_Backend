package com.fixzone.fixzon_backend.controller;

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
    public ResponseEntity<?> initPayment(@RequestBody com.fixzone.fixzon_backend.DTO.InitPaymentRequest initRequest) {
        try {
            com.fixzone.fixzon_backend.DTO.InitPaymentResponse response = paymentService.initPayment(initRequest);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error initializing payment: " + e.getMessage());
        }
    }

    @PostMapping(value = "/stripe", produces = org.springframework.http.MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> createStripeSession(@RequestBody com.fixzone.fixzon_backend.DTO.StripePaymentRequest stripeRequest) {
        try {
            String sessionUrl = paymentService.createStripeSession(stripeRequest.getPaymentId());
            return ResponseEntity.ok(sessionUrl);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating Stripe session: " + e.getMessage());
        }
    }

    @GetMapping("/success")
    public ResponseEntity<String> paymentSuccess(@RequestParam("session_id") String sessionId) {
        boolean success = paymentService.handleSuccess(sessionId);
        if (success) {
            return ResponseEntity.ok("Payment successful and status updated to PAID.");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment could not be verified.");
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable Long bookingId) {
        Payment payment = paymentService.getPaymentStatus(bookingId);
        if (payment != null) {
            return ResponseEntity.ok(payment);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Payment not found for bookingId: " + bookingId);
    }

    @PostMapping("/refund")
    public ResponseEntity<String> refundPayment(@RequestBody RefundRequest refundRequest) {
        boolean success = paymentService.refundPayment(refundRequest.getBookingId());
        if (success) {
            return ResponseEntity.ok("Refund processed successfully.");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to process refund.");
    }

    @PostMapping(value = "/reschedule", produces = org.springframework.http.MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> reschedulePayment(@RequestBody RescheduleRequest rescheduleRequest) {
        try {
            String newSessionUrl = paymentService.reschedulePayment(rescheduleRequest.getBookingId());
            return ResponseEntity.ok(newSessionUrl);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating reschedule session: " + e.getMessage());
        }
    }
}
