package com.fixzone.fixzon_backend.config;

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

import java.time.LocalDateTime;

@Component
public class SubscriptionInterceptor implements HandlerInterceptor {

    private final AuthRepository authRepository;
    private final OwnerRepository ownerRepository;

    public SubscriptionInterceptor(AuthRepository authRepository, OwnerRepository ownerRepository) {
        this.authRepository = authRepository;
        this.ownerRepository = ownerRepository;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        // Skip check for public endpoints, subscription endpoints, payment connect, and owner profile endpoints
        String path = request.getRequestURI();
        if (path.contains("/subscriptions/") || path.contains("/auth/") || path.contains("/payments/connect") || path.contains("/owners/")) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_COMPANY_OWNER"))) {

            String email = authentication.getName();
            Object userObj = authRepository.findByEmail(email).orElse(null);

            if (userObj instanceof Owner owner) {
                LocalDateTime now = LocalDateTime.now();

                boolean isTrialExpired = "TRIAL".equals(owner.getSubscriptionStatus()) &&
                        owner.getTrialEndsAt() != null &&
                        owner.getTrialEndsAt().isBefore(now);

                boolean isActiveExpired = "ACTIVE".equals(owner.getSubscriptionStatus()) &&
                        owner.getNextBillingDate() != null &&
                        owner.getNextBillingDate().isBefore(now);

                boolean isExpired = isTrialExpired || isActiveExpired || "EXPIRED".equals(owner.getSubscriptionStatus()) || "INACTIVE".equals(owner.getSubscriptionStatus());

                if (isExpired) {
                    // Persist INACTIVE status to DB so customers cannot see their branches
                    if (!"INACTIVE".equals(owner.getSubscriptionStatus())) {
                        owner.setSubscriptionStatus("INACTIVE");
                        owner.setStatus("Inactive");
                        ownerRepository.save(owner);
                    }
                    response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"SUBSCRIPTION_EXPIRED\",\"message\":\"Your subscription has expired. Please upgrade your plan to continue.\"}");
                    return false;
                }
            }
        }
        return true;
    }
}
