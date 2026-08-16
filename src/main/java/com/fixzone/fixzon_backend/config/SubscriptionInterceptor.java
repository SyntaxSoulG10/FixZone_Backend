package com.fixzone.fixzon_backend.config;

import com.fixzone.fixzon_backend.enums.SubscriptionStatus;
import com.fixzone.fixzon_backend.model.Owner;
import com.fixzone.fixzon_backend.repository.AuthRepository;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Real-time safety net that blocks API access for owners with expired
 * subscriptions.
 *
 * The daily cron job (SubscriptionScheduler) is the primary mechanism for
 * status
 * transitions, but this interceptor catches any expiry that happens between
 * cron runs.
 * If it detects an expired owner, it persists the new status and returns HTTP
 * 402.
 */
@Component
public class SubscriptionInterceptor implements HandlerInterceptor {

    private final AuthRepository authRepository;
    private final OwnerRepository ownerRepository;

    public SubscriptionInterceptor(AuthRepository authRepository, OwnerRepository ownerRepository) {
        this.authRepository = authRepository;
        this.ownerRepository = ownerRepository;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler) throws Exception {
        // Only block specific write operations, allow all GET requests
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method)) {
            return true;
        }

        String path = request.getRequestURI();

        // Define paths that are blocked for expired subscriptions
        boolean isBlockedWriteAction = (path.startsWith("/api/service-centers") && ("POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method))) ||
                (path.startsWith("/api/service-packages") && ("POST".equalsIgnoreCase(method)
                        || "PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)))
                ||
                (path.startsWith("/api/managers") && ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)
                        || "DELETE".equalsIgnoreCase(method)))
                ||
                (path.matches("^/api/bookings/[^/]+/cancel$") && "PUT".equalsIgnoreCase(method)) ||
                (path.equals("/api/payments/init") && "POST".equalsIgnoreCase(method)) ||
                (path.equals("/api/payments/stripe") && "POST".equalsIgnoreCase(method)) ||
                (path.equals("/api/payments/connect") && "POST".equalsIgnoreCase(method));

        if (!isBlockedWriteAction) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_COMPANY_OWNER"))) {

            String email = authentication.getName();
            Object userObj = authRepository.findByEmail(email).orElse(null);

            if (userObj instanceof Owner owner) {
                // Use the enum's fromLegacy() to handle both old and new status strings
                SubscriptionStatus status = SubscriptionStatus.fromLegacy(owner.getSubscriptionStatus());

                if (!status.isAccessAllowed()) {
                    // Persist the canonical status if it differs from what's in the DB
                    // This handles real-time detection between cron runs
                    if (!status.name().equals(owner.getSubscriptionStatus())) {
                        owner.setSubscriptionStatus(status.name());
                        owner.setStatus("Inactive");
                        ownerRepository.save(owner);
                    }

                    response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"error\":\"SUBSCRIPTION_EXPIRED\",\"message\":\"Your subscription has expired. Please upgrade your plan to continue.\"}");
                    return false;
                }

                // Real-time expiry check (safety net between cron runs)
                java.time.LocalDateTime now = java.time.LocalDateTime.now();

                boolean trialExpiredNow = status == SubscriptionStatus.TRIAL_ACTIVE &&
                        owner.getTrialEndsAt() != null && owner.getTrialEndsAt().isBefore(now);

                boolean premiumExpiredNow = status == SubscriptionStatus.PREMIUM_ACTIVE &&
                        owner.getNextBillingDate() != null && owner.getNextBillingDate().isBefore(now);

                if (trialExpiredNow) {
                    owner.setSubscriptionStatus(SubscriptionStatus.TRIAL_EXPIRED.name());
                    owner.setStatus("Inactive");
                    ownerRepository.save(owner);
                    return blockWithPaymentRequired(response);
                }

                if (premiumExpiredNow) {
                    owner.setSubscriptionStatus(SubscriptionStatus.PREMIUM_EXPIRED.name());
                    owner.setStatus("Inactive");
                    ownerRepository.save(owner);
                    return blockWithPaymentRequired(response);
                }
            }
        }
        return true;
    }

    private boolean blockWithPaymentRequired(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"error\":\"SUBSCRIPTION_EXPIRED\",\"message\":\"Your subscription has expired. Please upgrade your plan to continue.\"}");
        return false;
    }
}
