package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.service.SubscriptionService;
import com.stripe.exception.StripeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fixzone.fixzon_backend.model.SubscriptionBilling;
import com.fixzone.fixzon_backend.repository.SubscriptionBillingRepository;
import java.util.List;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {
    private static final Logger log = LoggerFactory.getLogger(SubscriptionController.class);

    private final SubscriptionService subscriptionService;
    private final SubscriptionBillingRepository subscriptionBillingRepository;
    private final com.fixzone.fixzon_backend.repository.SubscriptionRepository subscriptionRepository;

    public SubscriptionController(SubscriptionService subscriptionService, 
                                SubscriptionBillingRepository subscriptionBillingRepository,
                                com.fixzone.fixzon_backend.repository.SubscriptionRepository subscriptionRepository) {
        this.subscriptionService = subscriptionService;
        this.subscriptionBillingRepository = subscriptionBillingRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @GetMapping("/{id}/billing")
    public ResponseEntity<List<SubscriptionBilling>> getBillingHistory(@PathVariable UUID id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("=== DEBUG: Requested billing history for subscription ID: {} ===", id);
        List<SubscriptionBilling> all = subscriptionBillingRepository.findAll();
        log.info("=== DEBUG: Total billing rows in DB: {} ===", all.size());
        if (!all.isEmpty()) {
            log.info("=== DEBUG: First row SubID in DB: {} ===", all.get(0).getSubscriptionId());
        }
        List<SubscriptionBilling> history = subscriptionBillingRepository.findBySubscriptionIdOrderByPaymentDateDesc(id);
        log.info("=== DEBUG: Returned matching rows: {} ===", history.size());
        return ResponseEntity.ok(history);
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(Principal principal, @RequestBody Map<String, Object> payload) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User must be logged in");
        }
        try {
            UUID planId = UUID.fromString((String) payload.get("planId"));

            String url = subscriptionService.createSubscriptionCheckout(principal.getName(), planId);
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

    @GetMapping("/seed-billing")
    public ResponseEntity<String> seedBillingData() {
        java.util.List<com.fixzone.fixzon_backend.model.Subscription> subs = subscriptionRepository.findAll();
        int count = 0;
        for (com.fixzone.fixzon_backend.model.Subscription sub : subs) {
            SubscriptionBilling sb = new SubscriptionBilling();
            sb.setSubscriptionId(sub.getId());
            sb.setAmount(new java.math.BigDecimal("14900.00"));
            sb.setPaymentDate(java.time.LocalDateTime.now().minusDays(10));
            sb.setStatus("Paid");
            sb.setMethod("MasterCard **** 1234");
            sb.setInvoiceId("INV-TEST-002");
            subscriptionBillingRepository.save(sb);
            count++;
        }
        return ResponseEntity.ok("Successfully seeded " + count + " billing records!");
    }
}
