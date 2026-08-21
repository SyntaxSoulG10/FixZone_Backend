package com.fixzone.fixzon_backend.middleware;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller-level interceptor that checks @RequireRole annotations on controller classes and methods.
 * Provides granular, declarative Role-Based Access Control (RBAC) across endpoints.
 */
@Component
public class RoleAuthorizationInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RoleAuthorizationInterceptor.class);

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {

        // Skip non-controller handler methods (e.g., static resource handlers)
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // Method-level annotation takes precedence over class-level annotation
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }

        // If no @RequireRole is present, allow access (Spring Security filter rules still apply)
        if (requireRole == null) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            log.warn("Unauthorized access attempt to {}: user is not authenticated", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"Authentication is required to access this resource.\"}");
            return false;
        }

        // Extract user roles and normalize them
        Set<String> userAuthorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        // Check if user has at least one of the required roles
        boolean hasRequiredRole = Arrays.stream(requireRole.value())
                .anyMatch(required -> isRoleMatching(required, userAuthorities));

        if (!hasRequiredRole) {
            log.warn("Access denied for user {} to {}. Required: {}, Found: {}",
                    authentication.getName(), request.getRequestURI(), Arrays.toString(requireRole.value()), userAuthorities);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"FORBIDDEN\",\"message\":\"Access denied. You do not have permission to access this resource.\"}");
            return false;
        }

        return true;
    }

    /**
     * Checks if the required role matches any of the user's granted authorities.
     * Supports both with and without "ROLE_" prefix (e.g. "ROLE_SUPER_ADMIN" and "SUPER_ADMIN" match).
     */
    private boolean isRoleMatching(String requiredRole, Set<String> userAuthorities) {
        String normalizedRequired = requiredRole.toUpperCase();
        String roleWithPrefix = normalizedRequired.startsWith("ROLE_") ? normalizedRequired : "ROLE_" + normalizedRequired;
        String roleWithoutPrefix = normalizedRequired.startsWith("ROLE_") ? normalizedRequired.substring(5) : normalizedRequired;

        return userAuthorities.contains(normalizedRequired)
                || userAuthorities.contains(roleWithPrefix)
                || userAuthorities.contains(roleWithoutPrefix);
    }
}
