package com.fixzone.fixzon_backend.config;

import com.fixzone.fixzon_backend.middleware.RoleAuthorizationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final SubscriptionInterceptor subscriptionInterceptor;
    private final RoleAuthorizationInterceptor roleAuthorizationInterceptor;

    public WebMvcConfig(SubscriptionInterceptor subscriptionInterceptor,
                          RoleAuthorizationInterceptor roleAuthorizationInterceptor) {
        this.subscriptionInterceptor = subscriptionInterceptor;
        this.roleAuthorizationInterceptor = roleAuthorizationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Role authorization interceptor - checks @RequireRole annotations on controllers
        registry.addInterceptor(roleAuthorizationInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**", "/api/webhooks/**", "/api/payments/connect/callback", "/api/subscriptions/seed-billing");

        // Subscription interceptor - enforces active subscriptions for owner actions
        registry.addInterceptor(subscriptionInterceptor)
                .addPathPatterns("/api/**") // Apply to all API endpoints
                .excludePathPatterns("/api/auth/**", "/api/subscriptions/**", "/api/subscription-plans/**", "/api/payments/connect/**");
    }
}
