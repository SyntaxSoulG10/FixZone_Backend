package com.fixzone.fixzon_backend.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "stripe")
@Data
public class StripeConfig {
    private static final Logger log = LoggerFactory.getLogger(StripeConfig.class);

    private String secretKey;

    @PostConstruct
    public void init() {
        if (secretKey == null || secretKey.isEmpty()) {
            log.error(">>> STRIPE API KEY IS MISSING! Check application.properties <<<");
        } else {
            Stripe.apiKey = secretKey;
            log.info(">>> STRIPE API KEY INITIALIZED SUCCESSFULLY <<<");
        }
    }
}

