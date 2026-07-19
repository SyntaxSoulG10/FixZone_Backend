package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.service.PaymentService;
import com.fixzone.fixzon_backend.service.SubscriptionService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    private final PaymentService paymentService;
    private final SubscriptionService subscriptionService;

    public StripeWebhookController(PaymentService paymentService, SubscriptionService subscriptionService) {
        this.paymentService = paymentService;
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeEvent(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            log.error("Stripe webhook signature verification failed.", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Signature Verification Failed");
        } catch (Exception e) {
            log.error("Error parsing Stripe webhook payload.", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Payload");
        }

        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
            if (session != null) {
                log.info("Processing checkout session: {}", session.getId());
                try {
                    // Distinguish between Subscription and Booking based on clientReferenceId
                    if (session.getClientReferenceId() != null && session.getClientReferenceId().contains("_")) {
                        log.info("Processing as Subscription payment.");
                        subscriptionService.handleSubscriptionSuccess(session.getId());
                    } else {
                        log.info("Processing as Booking payment.");
                        paymentService.handleSuccess(session.getId());
                    }
                } catch (Exception e) {
                    log.error("Failed to process session completion for session: {}", session.getId(), e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Processing Failed");
                }
            }
        }

        return ResponseEntity.ok("Success");
    }
}
