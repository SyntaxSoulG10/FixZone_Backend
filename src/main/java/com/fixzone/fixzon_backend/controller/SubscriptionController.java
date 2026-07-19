package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.service.SubscriptionService;
import com.stripe.exception.StripeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {
    private static final Logger log = LoggerFactory.getLogger(SubscriptionController.class);

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(Principal principal, @RequestBody Map<String, Object> payload) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User must be logged in");
        }
        try {
            UUID planId = UUID.fromString((String) payload.get("planId"));
            boolean autoRenew = payload.containsKey("autoRenew") && (Boolean) payload.get("autoRenew");

            String url = subscriptionService.createSubscriptionCheckout(principal.getName(), planId, autoRenew);
            return ResponseEntity.ok(Map.of("checkoutUrl", url));
        } catch (StripeException e) {
            log.error("Stripe Subscription Error: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create subscription session");
        } catch (Exception e) {
            log.error("Subscription Checkout Error: ", e);
            return ResponseEntity.badRequest().body("Invalid request");
        }
    }

    @PostMapping("/success")
    public ResponseEntity<String> successCallback(@RequestParam("session_id") String sessionId) {
        try {
            subscriptionService.handleSubscriptionSuccess(sessionId);
            return ResponseEntity.ok("Subscription updated successfully");
        } catch (Exception e) {
            log.error("Subscription Callback Error: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing subscription");
        }
    }
}
