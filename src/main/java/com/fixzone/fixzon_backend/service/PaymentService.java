package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.InitPaymentRequest;
import com.fixzone.fixzon_backend.enums.BookingStatus;
import com.fixzone.fixzon_backend.enums.PaymentStatus;
import com.fixzone.fixzon_backend.enums.SubscriptionStatus;
import com.fixzone.fixzon_backend.model.Booking;
import com.fixzone.fixzon_backend.model.Payment;
import com.fixzone.fixzon_backend.model.ServicePackage;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.repository.BookingRepository;
import com.fixzone.fixzon_backend.repository.PaymentRepository;
import com.fixzone.fixzon_backend.repository.ServicePackageRepository;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.AuthRepository;
import com.fixzone.fixzon_backend.repository.CustomerRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.net.Webhook;
import com.fixzone.fixzon_backend.model.Owner;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service

public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Value("${stripe.secret-key}")
    private String stripeApiKey;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.backend-url}")
    private String backendUrl;

    @Value("${stripe.webhook.secret:whsec_test}")
    private String endpointSecret;

    private final PaymentRepository paymentRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final BookingRepository bookingRepository;
    private final AuthRepository authRepository;
    private final OwnerRepository ownerRepository;
    private final ServiceCenterRepository serviceCenterRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final CustomerRepository customerRepository;

    public PaymentService(PaymentRepository paymentRepository,
            ServicePackageRepository servicePackageRepository,
            BookingRepository bookingRepository,
            AuthRepository authRepository,
            OwnerRepository ownerRepository,
            ServiceCenterRepository serviceCenterRepository,
            NotificationService notificationService,
            EmailService emailService,
            CustomerRepository customerRepository) {
        this.paymentRepository = paymentRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.bookingRepository = bookingRepository;
        this.authRepository = authRepository;
        this.ownerRepository = ownerRepository;
        this.serviceCenterRepository = serviceCenterRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public Payment initPayment(InitPaymentRequest request, String bookingId, String customerEmail) {
        if (request.getServicePackageId() == null)
            throw new RuntimeException("Service Package ID is missing");

        UUID packageId = UUID.fromString(request.getServicePackageId());
        ServicePackage servicePackage = servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new RuntimeException("Service package not found"));

        double totalAmount = servicePackage.getBasePrice().doubleValue();
        double initialAmount = totalAmount * 0.4; // 40% initial payment

        Payment payment = new Payment();
        if (bookingId != null && !bookingId.isEmpty()) {
            try {
                payment.setBookingId(Long.parseLong(bookingId));
            } catch (NumberFormatException e) {
                // If it's a UUID string, it will be handled by the Detail Matcher during
                // session creation
            }
        }
        payment.setServicePackageId(packageId);
        payment.setVehicleId(UUID.fromString(request.getVehicleId()));

        // Resolve centerId and tenantId (owner) from the service package → service center chain.
        // This is the AUTHORITATIVE path — we never trust centerId/ownerId from the client request.
        if (servicePackage.getServiceCenter() != null) {
            ServiceCenter center = servicePackage.getServiceCenter();
            payment.setCenterId(center.getCenterId());

            // Resolve owner: prefer the eager relation; fall back to a DB look-up by centerId
            if (center.getOwner() != null) {
                // The owner field on ServiceCenter is typed as User — resolve the full Owner entity
                ownerRepository.findById(center.getOwner().getUserId()).ifPresent(owner ->
                        payment.setTenantId(owner.getUserId()));
            } else {
                // Fallback: look up owner directly from the service center record in DB
                serviceCenterRepository.findById(center.getCenterId()).ifPresent(sc -> {
                    if (sc.getOwner() != null) {
                        ownerRepository.findById(sc.getOwner().getUserId()).ifPresent(owner ->
                                payment.setTenantId(owner.getUserId()));
                    }
                });
            }
        } else if (request.getCenterId() != null && !request.getCenterId().isEmpty()) {
            // Service package does not have the center eagerly loaded — look it up
            UUID centerId = UUID.fromString(request.getCenterId());
            payment.setCenterId(centerId);
            serviceCenterRepository.findById(centerId).ifPresent(sc -> {
                if (sc.getOwner() != null) {
                    ownerRepository.findById(sc.getOwner().getUserId()).ifPresent(owner ->
                            payment.setTenantId(owner.getUserId()));
                }
            });
        }

        payment.setDate(request.getDate());
        payment.setTimeSlot(request.getTimeSlot());
        payment.setAmount(initialAmount);
        payment.setStatus(PaymentStatus.PENDING);

        // Set real customer ID if email is provided
        if (customerEmail != null) {
            authRepository.findByEmail(customerEmail).ifPresent(user -> {
                payment.setCustomerId(user.getUserId());
            });
        }

        // Validate BEFORE saving — throw a clear error if the owner has not completed
        // Stripe Connect so the frontend can show an actionable message immediately.
        ensurePayoutReady(payment.getTenantId());

        return paymentRepository.save(payment);
    }

    public Map<String, Object> validatePayoutEligibility(UUID tenantId) {
        if (tenantId == null) {
            return Map.of(
                    "eligible", false,
                    "stripeConnected", false,
                    "stripeAccountId", "",
                    "message", "This branch is not linked to an owner yet. Payment cannot proceed."
            );
        }

        Optional<Owner> ownerOpt = ownerRepository.findById(tenantId);
        if (ownerOpt.isEmpty()) {
            return Map.of(
                    "eligible", false,
                    "stripeConnected", false,
                    "stripeAccountId", "",
                    "message", "The branch owner could not be found. Payment cannot proceed."
            );
        }

        Owner owner = ownerOpt.get();
        boolean connected = Boolean.TRUE.equals(owner.getStripeOnboardingComplete())
                && owner.getStripeAccountId() != null
                && !owner.getStripeAccountId().isBlank();
        boolean subscriptionAllowed = SubscriptionStatus.fromLegacy(owner.getSubscriptionStatus()).isVisibleToCustomers();
        boolean eligible = connected && subscriptionAllowed;

        return Map.of(
                "eligible", eligible,
                "stripeConnected", connected,
                "stripeAccountId", owner.getStripeAccountId() != null ? owner.getStripeAccountId() : "",
                "message", eligible
                        ? "The branch owner is ready to receive online payments."
                        : (!connected
                                ? "This branch cannot accept online payments until the owner completes Stripe Connect onboarding."
                                : "This branch cannot accept online payments because the owner plan or trial is inactive.")
        );
    }

    private void ensurePayoutReady(UUID tenantId) {
        Map<String, Object> validation = validatePayoutEligibility(tenantId);
        if (!Boolean.TRUE.equals(validation.get("eligible"))) {
            throw new IllegalStateException((String) validation.get("message"));
        }
    }

    private String resolveStripeAccountId(UUID tenantId) {
        if (tenantId == null) {
            return null;
        }

        Optional<Owner> ownerOpt = ownerRepository.findById(tenantId);
        if (ownerOpt.isPresent()) {
            Owner owner = ownerOpt.get();
            if (owner.getStripeAccountId() != null && Boolean.TRUE.equals(owner.getStripeOnboardingComplete())) {
                return owner.getStripeAccountId();
            }
        }
        return null;
    }

    public String createStripeSession(Long paymentId) throws StripeException {
        Stripe.apiKey = stripeApiKey;

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment record not found"));

        ensurePayoutReady(payment.getTenantId());

        try {
            String stripeAccountId = resolveStripeAccountId(payment.getTenantId());
            if (stripeAccountId == null) {
                throw new IllegalStateException("This branch cannot accept online payments until the owner completes Stripe Connect onboarding.");
            }

            String frontendBaseUrl = resolveBaseUrl(null, frontendUrl, "http://localhost:3000");

            SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(frontendBaseUrl + "/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendBaseUrl + "/dashboard/customer/checkout")
                .addLineItem(SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency("lkr")
                        .setUnitAmount((long) (payment.getAmount() * 100))
                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                            .setName("Service Booking Fee")
                            .build())
                        .build())
                    .build())
                .setPaymentIntentData(
                    SessionCreateParams.PaymentIntentData.builder()
                        .setTransferData(
                            SessionCreateParams.PaymentIntentData.TransferData.builder()
                                .setDestination(stripeAccountId)
                                .build()
                        )
                        .build()
                )
                .build();

            log.info(">>> ROUTING PAYMENT VIA DESTINATION CHARGE TO: {}", stripeAccountId);
            Session session = Session.create(params); // No RequestOptions - platform account handles charge

            payment.setGatewaySessionId(session.getId());
            paymentRepository.save(payment);

            // Robust Search: Find the booking by ID or by matching service details
            Optional<Booking> bookingOpt = bookingRepository.findAll().stream()
                    .filter(b -> {
                        if (b.getBookingId() == null)
                            return false;

                        // 1. Try matching by Booking ID
                        String bId = b.getBookingId().toString();
                        String pId = payment.getBookingId() != null ? payment.getBookingId().toString() : "";
                        if (bId.equalsIgnoreCase(pId))
                            return true;

                        // 2. Fallback: Match by Service Package + Vehicle + Date
                        boolean packageMatch = b.getPackageId() != null
                                && b.getPackageId().equals(payment.getServicePackageId());
                        boolean vehicleMatch = b.getVehicleId() != null
                                && b.getVehicleId().equals(payment.getVehicleId());
                        boolean dateMatch = b.getBookingDate() != null
                                && b.getBookingDate().toString().equals(payment.getDate());

                        return packageMatch && vehicleMatch && dateMatch;
                    })
                    .findFirst();

            if (bookingOpt.isPresent()) {
                Booking booking = bookingOpt.get();
                log.info(">>> LINKING STRIPE SESSION TO BOOKING: {}", booking.getBookingId());
                booking.setGatewaySessionId(session.getId());
                booking.setBookingFee(BigDecimal.valueOf(payment.getAmount()));
                
                // Set Estimated Cost from Service Package
                servicePackageRepository.findById(payment.getServicePackageId()).ifPresent(pkg -> {
                    booking.setEstimatedCost(pkg.getBasePrice());
                });

                bookingRepository.save(booking);
            }
            return session.getUrl();
        } catch (StripeException e) {
            log.error("STRIPE ERROR: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public PaymentStatus getPaymentStatusById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) return null;

        if (payment.getStatus() == PaymentStatus.PENDING && payment.getGatewaySessionId() != null && payment.getGatewaySessionId().startsWith("pi_")) {
            try {
                Stripe.apiKey = stripeApiKey;
                PaymentIntent intent = PaymentIntent.retrieve(payment.getGatewaySessionId());
                if ("succeeded".equals(intent.getStatus())) {
                    log.info(">>> FALLBACK CHECK: PaymentIntent {} succeeded. Updating DB.", intent.getId());
                    payment.setStatus(PaymentStatus.PAID);
                    paymentRepository.save(payment);

                    confirmBookingForPayment(payment, payment.getGatewaySessionId());
                }
            } catch (StripeException e) {
                log.error("Error retrieving PaymentIntent from Stripe during fallback check", e);
            }
        }
        return payment.getStatus();
    }

    public Map<String, Object> createStripePaymentIntent(Long paymentId) throws StripeException {
        Stripe.apiKey = stripeApiKey;
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment record not found"));

        ensurePayoutReady(payment.getTenantId());

        String stripeAccountId = resolveStripeAccountId(payment.getTenantId());
        if (stripeAccountId == null) {
            throw new IllegalStateException("This branch cannot accept online payments until the owner completes Stripe Connect onboarding.");
        }

        // Idempotency: If gatewaySessionId already exists and is a PaymentIntent
        if (payment.getGatewaySessionId() != null && payment.getGatewaySessionId().startsWith("pi_")) {
            log.info(">>> REUSING EXISTING PAYMENT INTENT: {}", payment.getGatewaySessionId());
            PaymentIntent intent = PaymentIntent.retrieve(payment.getGatewaySessionId());
            return Map.of(
                    "paymentId", payment.getId(),
                    "clientSecret", intent.getClientSecret(),
                    "amount", payment.getAmount(),
                    "currency", "LKR"
            );
        }

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount((long) (payment.getAmount() * 100))
                .setCurrency("lkr")
                .putMetadata("paymentId", payment.getId().toString())
                .putMetadata("bookingId", payment.getBookingId() != null ? payment.getBookingId().toString() : "")
                .putMetadata("branchId", payment.getCenterId() != null ? payment.getCenterId().toString() : "")
                .putMetadata("customerId", payment.getCustomerId() != null ? payment.getCustomerId().toString() : "")
                .setTransferData(
                        PaymentIntentCreateParams.TransferData.builder()
                                .setDestination(stripeAccountId)
                                .build()
                )
                .build();

        log.info(">>> CREATING NEW PAYMENT INTENT (DESTINATION: {})", stripeAccountId);
        PaymentIntent intent = PaymentIntent.create(params);

        payment.setGatewaySessionId(intent.getId());
        paymentRepository.save(payment);

        linkPaymentToBooking(payment, intent.getId());

        return Map.of(
                "paymentId", payment.getId(),
                "clientSecret", intent.getClientSecret(),
                "amount", payment.getAmount(),
                "currency", "LKR"
        );
    }

    private void linkPaymentToBooking(Payment payment, String gatewayId) {
        Optional<Booking> bookingOpt = bookingRepository.findAll().stream()
                .filter(b -> {
                    if (b.getBookingId() == null) return false;
                    String bId = b.getBookingId().toString();
                    String pId = payment.getBookingId() != null ? payment.getBookingId().toString() : "";
                    if (bId.equalsIgnoreCase(pId)) return true;

                    boolean packageMatch = b.getPackageId() != null && b.getPackageId().equals(payment.getServicePackageId());
                    boolean vehicleMatch = b.getVehicleId() != null && b.getVehicleId().equals(payment.getVehicleId());
                    boolean dateMatch = b.getBookingDate() != null && b.getBookingDate().toString().equals(payment.getDate());
                    return packageMatch && vehicleMatch && dateMatch;
                })
                .findFirst();

        if (bookingOpt.isPresent()) {
            Booking booking = bookingOpt.get();
            booking.setGatewaySessionId(gatewayId);
            booking.setBookingFee(BigDecimal.valueOf(payment.getAmount()));
            servicePackageRepository.findById(payment.getServicePackageId()).ifPresent(pkg -> {
                booking.setEstimatedCost(pkg.getBasePrice());
            });
            bookingRepository.save(booking);
        }
    }

    @Transactional
    public void handleStripeWebhook(String payload, String sigHeader) throws Exception {
        Stripe.apiKey = stripeApiKey;
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (Exception e) {
            log.error("Webhook signature verification failed.", e);
            throw e;
        }

        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = dataObjectDeserializer.getObject().orElse(null);

        if ("payment_intent.succeeded".equals(event.getType()) && stripeObject instanceof PaymentIntent) {
            PaymentIntent intent = (PaymentIntent) stripeObject;
            String paymentIntentId = intent.getId();
            log.info(">>> WEBHOOK: PaymentIntent Succeeded for {}", paymentIntentId);
            
            Optional<Payment> paymentOpt = paymentRepository.findAll().stream()
                    .filter(p -> paymentIntentId.equals(p.getGatewaySessionId()))
                    .findFirst();

            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                payment.setStatus(PaymentStatus.PAID);
                paymentRepository.save(payment);

                confirmBookingForPayment(payment, paymentIntentId);
            }
        }
    }

    @Transactional
    public boolean handleSuccess(String sessionId) {
        Stripe.apiKey = stripeApiKey;
        try {
            // 1. Find local payment record first to determine if we need Stripe Connect RequestOptions
            Optional<Payment> paymentOpt = paymentRepository.findAll().stream()
                    .filter(p -> sessionId.equals(p.getGatewaySessionId()))
                    .findFirst();

            if (paymentOpt.isEmpty()) {
                log.warn(">>> PAYMENT RECORD NOT FOUND FOR SESSION: {}", sessionId);
                return false;
            }

            Payment payment = paymentOpt.get();
            
            Session session = Session.retrieve(sessionId); // Platform account owns the session now

            if ("paid".equalsIgnoreCase(session.getPaymentStatus())) {
                payment.setStatus(PaymentStatus.PAID);
                paymentRepository.save(payment);

                // Update Booking (Find existing or create new)
                Optional<Booking> bookingOpt = bookingRepository.findAll().stream()
                        .filter(b -> sessionId.equals(b.getGatewaySessionId()))
                        .findFirst();

                Booking booking;
                if (bookingOpt.isPresent()) {
                    booking = bookingOpt.get();
                } else {
                    // Create NEW Booking from Payment details
                    booking = new Booking();
                    booking.setBookingId(UUID.randomUUID());

                    if (payment.getCustomerId() != null) {
                        booking.setCustomerId(payment.getCustomerId());
                    } else {
                        throw new RuntimeException("Cannot create booking: Customer ID is missing from payment record");
                    }

                    booking.setTenantId(payment.getTenantId());
                    booking.setCenterId(payment.getCenterId());
                    booking.setVehicleId(payment.getVehicleId());
                    booking.setPackageId(payment.getServicePackageId());
                    booking.setBookingDate(java.time.LocalDate.parse(payment.getDate()));

                    String timeStr = payment.getTimeSlot();
                    if (timeStr != null && timeStr.contains("-")) {
                        timeStr = timeStr.split("-")[0].trim();
                    }
                    booking.setBookingTime(java.time.LocalTime.parse(timeStr));
                    booking.setGatewaySessionId(sessionId);
                    booking.setBookingFee(BigDecimal.valueOf(payment.getAmount()));
                    booking.setCenterId(payment.getCenterId());
                    booking.setTenantId(payment.getTenantId() != null ? payment.getTenantId() : UUID.randomUUID());

                    servicePackageRepository.findById(payment.getServicePackageId()).ifPresent(pkg -> {
                        booking.setEstimatedCost(pkg.getBasePrice());
                    });
                }

                booking.setStatus(BookingStatus.CONFIRMED);
                booking.setBookingFeePaid(true);
                Booking savedBooking = bookingRepository.save(booking);

                sendPaymentSuccessNotifications(savedBooking, payment);
                return true;
            }
        } catch (StripeException e) {
            log.error("SUCCESS VERIFICATION ERROR: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean refundPayment(String gatewaySessionId, double penaltyPercentage) {
        Stripe.apiKey = stripeApiKey;
        try {
            Session session = Session.retrieve(gatewaySessionId); // Platform account owns session

            String paymentIntentId = session.getPaymentIntent();

            long refundAmount = session.getAmountTotal();
            if (penaltyPercentage > 0) {
                refundAmount = (long) (refundAmount * (1 - (penaltyPercentage / 100)));
            }

            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntentId)
                    .setAmount(refundAmount)
                    .build();

            Refund.create(params); // Refund from platform account
            return true;
        } catch (StripeException e) {
            log.error("REFUND ERROR: {}", e.getMessage(), e);
            return false;
        }
    }

    public Payment getPaymentStatus(Long bookingId) {
        return paymentRepository.findAll().stream()
                .filter(p -> p.getBookingId() != null && p.getBookingId().equals(bookingId))
                .findFirst()
                .orElse(null);
    }

    public String reschedulePayment(Long bookingId) throws StripeException {
        Payment payment = getPaymentStatus(bookingId);
        if (payment == null) {
            throw new RuntimeException("Payment not found for booking");
        }
        return createStripeSession(payment.getId());
    }

    public String resolveBaseUrl(HttpServletRequest request, String configuredBaseUrl, String fallbackBaseUrl) {
        String base = configuredBaseUrl;
        if (base != null && !base.isBlank()) {
            base = base.trim().replaceAll("^[\"']|[\"']$", "").replaceAll("/+$", "");
            if (!base.startsWith("http://") && !base.startsWith("https://")) {
                base = "https://" + base;
            }
            return base;
        }

        if (request == null) {
            String fallback = fallbackBaseUrl != null ? fallbackBaseUrl.trim().replaceAll("^[\"']|[\"']$", "").replaceAll("/+$", "") : "";
            return fallback;
        }

        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        if (forwardedProto != null && !forwardedProto.isBlank() && forwardedHost != null && !forwardedHost.isBlank()) {
            String proto = forwardedProto.split(",")[0].trim();
            String host = forwardedHost.split(",")[0].trim();
            return proto + "://" + host;
        }

        String scheme = request.getScheme();
        int port = request.getServerPort();
        String host = request.getServerName();
        if (port == 80 || port == 443) {
            return scheme + "://" + host;
        }
        return scheme + "://" + host + ":" + port;
    }

    public String generateConnectLink(String ownerEmail, HttpServletRequest request) throws StripeException {
        if (stripeApiKey == null || stripeApiKey.isBlank()) {
            throw new IllegalStateException("Stripe is not configured on the backend.");
        }

        Stripe.apiKey = stripeApiKey;
        Owner owner = ownerRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        if (owner.getStripeAccountId() == null || owner.getStripeAccountId().isBlank()) {
            try {
                AccountCreateParams params = AccountCreateParams.builder()
                        .setType(AccountCreateParams.Type.EXPRESS)
                        .setCountry("LK")
                        .setEmail(owner.getEmail())
                        .build();
                Account account = Account.create(params);
                owner.setStripeAccountId(account.getId());
                ownerRepository.save(owner);
            } catch (StripeException e) {
                String friendlyMessage = buildConnectErrorMessage(e.getMessage());
                log.error("Failed to create Stripe Connect account for owner {}: {}", owner.getEmail(), e.getMessage(), e);
                throw new IllegalStateException(friendlyMessage, e);
            }
        }

        String frontendBaseUrl = resolveBaseUrl(request, frontendUrl, "http://localhost:3000");
        String backendBaseUrl = resolveBaseUrl(request, backendUrl, "http://localhost:8081");

        String encodedAccountId = java.net.URLEncoder.encode(
                owner.getStripeAccountId() != null ? owner.getStripeAccountId().trim() : "",
                java.nio.charset.StandardCharsets.UTF_8
        );

        String refreshUrl = frontendBaseUrl + "/dashboard/company-owner/centers?stripe_refresh=true";
        String returnUrl = backendBaseUrl + "/api/payments/connect/callback?accountId=" + encodedAccountId;

        log.info("Generating Stripe Connect Link for account '{}': refreshUrl='{}', returnUrl='{}'", 
                owner.getStripeAccountId(), refreshUrl, returnUrl);

        AccountLinkCreateParams linkParams = AccountLinkCreateParams.builder()
                .setAccount(owner.getStripeAccountId().trim())
                .setRefreshUrl(refreshUrl)
                .setReturnUrl(returnUrl)
                .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                .build();

        AccountLink accountLink = AccountLink.create(linkParams);
        return accountLink.getUrl();
    }

    String buildConnectErrorMessage(String stripeMessage) {
        String message = stripeMessage == null ? "" : stripeMessage;
        if (message.contains("signed up for Connect") || message.contains("Connect")) {
            return "Stripe Connect is not enabled for this Stripe account. Please enable Connect in your Stripe dashboard and try again.";
        }
        return "Unable to start Stripe Connect onboarding right now. " + message;
    }

    public void handleConnectCallback(String accountId) throws StripeException {
        Stripe.apiKey = stripeApiKey;
        Account account = Account.retrieve(accountId);
        if (Boolean.TRUE.equals(account.getChargesEnabled()) && Boolean.TRUE.equals(account.getPayoutsEnabled())) {
            Owner owner = ownerRepository.findAll().stream()
                    .filter(o -> accountId.equals(o.getStripeAccountId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Owner not found for account"));
            owner.setStripeOnboardingComplete(true);
            ownerRepository.save(owner);
        }
    }

    /**
     * Returns the Stripe Connect status for the currently authenticated owner.
     * Used by the branch creation form to show the connect step when needed.
     */
    public org.springframework.http.ResponseEntity<?> getConnectStatus(String ownerEmail) {
        Optional<Owner> ownerOpt = ownerRepository.findByEmail(ownerEmail);

        if (ownerOpt.isEmpty()) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(java.util.Map.of("error", "Owner not found"));
        }

        Owner owner = ownerOpt.get();
        boolean connected = Boolean.TRUE.equals(owner.getStripeOnboardingComplete());

        return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                "stripeConnected", connected,
                "stripeAccountId", owner.getStripeAccountId() != null ? owner.getStripeAccountId() : "",
                "message", connected
                        ? "Stripe account connected. You can create branches."
                        : "Stripe account not connected. You must complete Stripe Connect setup before creating a branch."
        ));
    }

    private void confirmBookingForPayment(Payment payment, String gatewaySessionId) {
        Optional<Booking> bookingOpt = bookingRepository.findAll().stream()
                .filter(b -> gatewaySessionId.equals(b.getGatewaySessionId()))
                .findFirst();

        Booking booking;
        if (bookingOpt.isPresent()) {
            booking = bookingOpt.get();
        } else {
            booking = new Booking();
            booking.setBookingId(UUID.randomUUID());
            if (payment.getCustomerId() != null) {
                booking.setCustomerId(payment.getCustomerId());
            } else {
                booking.setCustomerId(UUID.randomUUID());
            }
            booking.setTenantId(payment.getTenantId());
            booking.setCenterId(payment.getCenterId());
            booking.setVehicleId(payment.getVehicleId());
            booking.setPackageId(payment.getServicePackageId());
            if (payment.getDate() != null) {
                booking.setBookingDate(java.time.LocalDate.parse(payment.getDate()));
            }

            String timeStr = payment.getTimeSlot();
            if (timeStr != null) {
                if (timeStr.contains("-")) {
                    timeStr = timeStr.split("-")[0].trim();
                }
                booking.setBookingTime(java.time.LocalTime.parse(timeStr));
            }
            booking.setGatewaySessionId(gatewaySessionId);
            booking.setBookingFee(BigDecimal.valueOf(payment.getAmount()));
            
            if (payment.getServicePackageId() != null) {
                servicePackageRepository.findById(payment.getServicePackageId()).ifPresent(pkg -> {
                    booking.setEstimatedCost(pkg.getBasePrice());
                });
            }
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookingFeePaid(true);
        Booking savedBooking = bookingRepository.save(booking);

        sendPaymentSuccessNotifications(savedBooking, payment);
    }

    private void sendPaymentSuccessNotifications(Booking booking, Payment payment) {
        if (booking == null) return;

        // Send In-App Notification & Confirmation Email to Customer
        if (booking.getCustomerId() != null) {
            authRepository.findById(booking.getCustomerId()).ifPresent(customer -> {
                log.info(">>> SENDING PAYMENT CONFIRMATION EMAIL & NOTIFICATION TO {}", customer.getEmail());
                notificationService.createNotificationSafe(customer, "Payment Successful",
                        "Your payment of LKR " + booking.getBookingFee() + " is confirmed for booking on " + booking.getBookingDate() + ".",
                        "SUCCESS", "/bookings");

                // Send Confirmation Email
                String pkgName = "Service Package";
                if (booking.getPackageId() != null) {
                    var pkgOpt = servicePackageRepository.findById(booking.getPackageId());
                    if (pkgOpt.isPresent()) pkgName = pkgOpt.get().getName();
                }
                String centerName = "Service Center";
                if (booking.getCenterId() != null) {
                    var centerOpt = serviceCenterRepository.findById(booking.getCenterId());
                    if (centerOpt.isPresent()) centerName = centerOpt.get().getName();
                }

                emailService.sendBookingConfirmationEmail(
                        customer.getEmail(),
                        customer.getFullName(),
                        pkgName,
                        centerName,
                        booking.getBookingDate() != null ? booking.getBookingDate().toString() : "",
                        booking.getBookingTime() != null ? booking.getBookingTime().toString() : "",
                        booking.getBookingFee() != null ? booking.getBookingFee() : BigDecimal.ZERO
                );
            });
        }

        // Send In-App Notification to Owner
        if (booking.getCenterId() != null) {
            serviceCenterRepository.findById(booking.getCenterId()).ifPresent(sc -> {
                if (sc.getOwner() != null) {
                    notificationService.createNotificationSafe(sc.getOwner(), "Booking Paid",
                            "Payment confirmed for booking at " + sc.getName() + " on " + booking.getBookingDate() + ".",
                            "SUCCESS", "/dashboard/company-owner/centers");
                }
            });
        }
    }

    /**
     * Finds the internal payment record ID for a given booking UUID.
     * Strategy:
     *  1. If the booking has a gatewaySessionId, find the payment by that session ID.
     *  2. Otherwise, find the most recent payment matching package + vehicle + date.
     */
    public Long findPaymentIdByBookingUUID(UUID bookingUUID) {
        Booking booking = bookingRepository.findById(bookingUUID)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingUUID));

        // Strategy 1: match by Stripe session ID stored on the booking
        if (booking.getGatewaySessionId() != null && !booking.getGatewaySessionId().isBlank()) {
            Optional<Payment> bySession = paymentRepository.findByGatewaySessionId(booking.getGatewaySessionId());
            if (bySession.isPresent()) {
                return bySession.get().getId();
            }
        }

        // Strategy 2: find the most recent payment matching package + vehicle + date
        if (booking.getPackageId() != null && booking.getVehicleId() != null && booking.getBookingDate() != null) {
            String dateStr = booking.getBookingDate().toString();
            return paymentRepository.findAll().stream()
                    .filter(p ->
                        booking.getPackageId().equals(p.getServicePackageId()) &&
                        booking.getVehicleId().equals(p.getVehicleId()) &&
                        dateStr.equals(p.getDate())
                    )
                    .max(java.util.Comparator.comparing(Payment::getId))
                    .map(Payment::getId)
                    .orElse(null);
        }

        return null;
    }
}
