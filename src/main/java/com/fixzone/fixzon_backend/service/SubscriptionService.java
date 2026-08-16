package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.model.Owner;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.model.SubscriptionPlan;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.SubscriptionPlanRepository;

import com.fixzone.fixzon_backend.repository.SubscriptionRepository;
import com.fixzone.fixzon_backend.repository.SubscriptionBillingRepository;
import com.fixzone.fixzon_backend.model.Subscription;
import com.fixzone.fixzon_backend.model.SubscriptionBilling;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class SubscriptionService {
    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    @Value("${stripe.secret-key}")
    private String stripeApiKey;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final OwnerRepository ownerRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final ServiceCenterRepository serviceCenterRepository;

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionBillingRepository subscriptionBillingRepository;

    public SubscriptionService(OwnerRepository ownerRepository,
                               SubscriptionPlanRepository subscriptionPlanRepository,
                               ServiceCenterRepository serviceCenterRepository,
                               SubscriptionRepository subscriptionRepository,
                               SubscriptionBillingRepository subscriptionBillingRepository) {
        this.ownerRepository = ownerRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.serviceCenterRepository = serviceCenterRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionBillingRepository = subscriptionBillingRepository;
    }

    private String resolveFrontendUrl() {
        String base = frontendUrl;
        if (base != null && !base.isBlank()) {
            base = base.trim().replaceAll("^[\"']|[\"']$", "").replaceAll("/+$", "");
            if (!base.startsWith("http://") && !base.startsWith("https://")) {
                base = "https://" + base;
            }
            return base;
        }
        return "http://localhost:3000";
    }

    public String createSubscriptionCheckout(String ownerEmail, UUID planId, boolean autoRenew) throws StripeException {
        Stripe.apiKey = stripeApiKey;

        Owner owner = ownerRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        String baseUrl = resolveFrontendUrl();
        String successUrl = baseUrl + "/dashboard/company-owner/profile?tab=billing&sub_success=true&session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = baseUrl + "/dashboard/company-owner/profile?tab=billing&sub_canceled=true";

        log.info("Creating Stripe Subscription Checkout for owner '{}', plan '{}': successUrl='{}', cancelUrl='{}'",
                owner.getEmail(), plan.getName(), successUrl, cancelUrl);

        SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setClientReferenceId(owner.getUserId().toString() + "_" + planId.toString() + "_" + autoRenew)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("lkr")
                                .setUnitAmount(plan.getPrice().longValue() * 100)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(plan.getName() + " Subscription")
                                        .build())
                                .build())
                        .build());

        // Save card details for future off-session charging if auto-renew is selected
        if (autoRenew) {
            paramsBuilder.setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder()
                    .setSetupFutureUsage(SessionCreateParams.PaymentIntentData.SetupFutureUsage.OFF_SESSION)
                    .build());
        }

        Session session = Session.create(paramsBuilder.build());
        return session.getUrl();
    }

    public void handleSubscriptionSuccess(String sessionId) throws StripeException {
        Stripe.apiKey = stripeApiKey;
        Session session = Session.retrieve(sessionId);

        if ("paid".equalsIgnoreCase(session.getPaymentStatus())) {
            String clientReferenceId = session.getClientReferenceId();
            if (clientReferenceId != null && clientReferenceId.contains("_")) {
                String[] parts = clientReferenceId.split("_");
                UUID ownerId = UUID.fromString(parts[0]);
                UUID planId = UUID.fromString(parts[1]);
                boolean autoRenew = Boolean.parseBoolean(parts[2]);

                Optional<Owner> ownerOpt = ownerRepository.findById(ownerId);
                Optional<SubscriptionPlan> planOpt = subscriptionPlanRepository.findById(planId);

                if (ownerOpt.isPresent() && planOpt.isPresent()) {
                    Owner owner = ownerOpt.get();
                    SubscriptionPlan plan = planOpt.get();

                    owner.setSubscriptionStatus("PREMIUM_ACTIVE");
                    owner.setStatus("Active"); // Reactivate owner account on successful subscription
                    owner.setCurrentPlanId(planId);
                    owner.setAutoRenewEnabled(autoRenew);
                    
                    if (session.getCustomer() != null) {
                        owner.setStripeCustomerId(session.getCustomer());
                    }

                    // Extend billing date by duration
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime startDate = (owner.getNextBillingDate() != null && owner.getNextBillingDate().isAfter(now)) 
                            ? owner.getNextBillingDate() 
                            : now;
                    
                    owner.setNextBillingDate(startDate.plusMonths(plan.getDurationMonths()));
                    ownerRepository.save(owner);
                    log.info("Subscription updated for owner {}", owner.getEmail());

                    // Reactivate any SUSPENDED service centers for this owner
                    List<ServiceCenter> ownerCenters = serviceCenterRepository.findByOwner_UserId(owner.getUserId());
                    for (ServiceCenter center : ownerCenters) {
                        if ("SUSPENDED".equalsIgnoreCase(center.getStatus())) {
                            center.setStatus("APPROVED");
                            center.setIsActive(true);
                            serviceCenterRepository.save(center);
                            log.info("Reactivated service center '{}' for owner {}", center.getName(), owner.getEmail());
                        }
                    }

                    // Handle Subscription entity
                    Subscription subscription = subscriptionRepository.findByOwnerUserId(owner.getUserId())
                            .orElseGet(() -> {
                                Subscription newSub = new Subscription();
                                newSub.setOwner(owner);
                                return newSub;
                            });
                    subscription.setPlan(plan);
                    subscription.setStartDate(startDate.toLocalDate());
                    subscription.setEndDate(owner.getNextBillingDate().toLocalDate());
                    subscription.setStatus("ACTIVE");
                    subscriptionRepository.save(subscription);

                    // Create Billing Record
                    SubscriptionBilling billing = new SubscriptionBilling();
                    billing.setSubscriptionId(subscription.getId());
                    billing.setAmount(plan.getPrice());
                    billing.setPaymentDate(now);
                    billing.setStatus("Success");
                    billing.setMethod("Stripe Checkout");
                    
                    // Retrieve invoice ID or payment intent from Stripe session if available
                    String invoiceId = session.getInvoice();
                    if (invoiceId == null) {
                        invoiceId = session.getPaymentIntent();
                        if (invoiceId == null) {
                            invoiceId = "INV-" + session.getId().substring(Math.max(0, session.getId().length() - 8));
                        }
                    }
                    billing.setInvoiceId(invoiceId);

                    subscriptionBillingRepository.save(billing);
                    log.info("Created billing record for owner {}", owner.getEmail());
                }
            }
        }
    }

    @Scheduled(cron = "0 0 0 * * ?") // Run at midnight every day
    public void checkSubscriptionExpirations() {
        log.info("Running daily subscription expiration check...");
        LocalDateTime now = LocalDateTime.now();

        // Expire trials
        List<Owner> expiredTrials = ownerRepository.findBySubscriptionStatusAndTrialEndsAtBefore("TRIAL_ACTIVE", now);
        for (Owner owner : expiredTrials) {
            log.info("Trial expired for owner: {}", owner.getEmail());
            owner.setSubscriptionStatus("TRIAL_EXPIRED");
            ownerRepository.save(owner);
        }

        // Expire premium plans where auto-renew is false
        List<Owner> expiredPremiums = ownerRepository.findBySubscriptionStatusAndNextBillingDateBeforeAndAutoRenewEnabled("PREMIUM_ACTIVE", now, false);
        for (Owner owner : expiredPremiums) {
            log.info("Premium subscription expired for owner: {}", owner.getEmail());
            owner.setSubscriptionStatus("PREMIUM_EXPIRED");
            ownerRepository.save(owner);
        }

        log.info("Completed daily subscription expiration check. Updated {} trials and {} premium plans.", expiredTrials.size(), expiredPremiums.size());
    }
}
