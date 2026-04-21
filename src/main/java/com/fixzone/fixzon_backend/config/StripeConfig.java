package com.fixzone.fixzon_backend.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "stripe")
@Data
public class StripeConfig {

    private String secretKey;

    @PostConstruct
    public void init() {
        if (secretKey == null || secretKey.isEmpty()) {
            System.err.println(">>> STRIPE API KEY IS MISSING! Check application.properties <<<");
        } else {
            Stripe.apiKey = secretKey;
            System.out.println(">>> STRIPE API KEY INITIALIZED SUCCESSFULLY <<<");
        }
    }
}

