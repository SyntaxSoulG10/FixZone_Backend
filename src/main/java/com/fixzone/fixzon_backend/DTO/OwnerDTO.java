package com.fixzone.fixzon_backend.DTO;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OwnerDTO extends UserDTO {
    private String ownerCode;

    @jakarta.validation.constraints.Size(min = 2, max = 150, message = "Company name must be 2-150 characters")
    private String companyName;

    @jakarta.validation.constraints.Pattern(regexp = "^$|^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Invalid company email format")
    private String companyEmail;

    @jakarta.validation.constraints.Pattern(regexp = "^$|^[0-9+()\\s-]{9,20}$", message = "Company phone must be 9-20 digits")
    private String companyNumber;
    private String bannerImageUrl;
    private String facebookUrl;
    private String twitterUrl;
    private String instagramUrl;

    // Stripe Connect & Subscription fields
    private Boolean stripeOnboardingComplete;
    private String subscriptionStatus;
    private java.time.LocalDateTime trialEndsAt;
    private java.time.LocalDateTime nextBillingDate;
    private Boolean autoRenewEnabled;
}
