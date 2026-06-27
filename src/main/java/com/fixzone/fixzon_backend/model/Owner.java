package com.fixzone.fixzon_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "owner")
@PrimaryKeyJoinColumn(name = "user_id")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Owner extends User {

    @Column(name = "owner_code", unique = true, nullable = false, length = 50)
    private String ownerCode;

    @Column(name = "company_name", length = 150)
    private String companyName;

    @Column(name = "company_email", length = 150)
    private String companyEmail;

    @Column(name = "company_number", length = 20)
    private String companyNumber;

    @Column(name = "banner_image_url", columnDefinition = "TEXT")
    private String bannerImageUrl;

    @Column(name = "facebook_url", length = 255)
    private String facebookUrl;

    @Column(name = "twitter_url", length = 255)
    private String twitterUrl;

    @Column(name = "instagram_url", length = 255)
    private String instagramUrl;

    @Column(name = "stripe_account_id", length = 100)
    private String stripeAccountId;

    @Column(name = "stripe_onboarding_complete")
    private Boolean stripeOnboardingComplete = false;

    // Subscription tracking fields
    @Column(name = "subscription_status", length = 50)
    private String subscriptionStatus;

    @Column(name = "trial_ends_at")
    private LocalDateTime trialEndsAt;

    @Column(name = "next_billing_date")
    private LocalDateTime nextBillingDate;

    @Column(name = "current_plan_id")
    private UUID currentPlanId;

    @Column(name = "stripe_customer_id", length = 100)
    private String stripeCustomerId;

    @Column(name = "auto_renew_enabled")
    private Boolean autoRenewEnabled = false;

    public Owner(UUID userId, String fullName, String email, String phone, String passwordHash, String role,
            Boolean emailVerified, LocalDateTime lastLoginAt, LocalDateTime createdAt, String createdBy,
            LocalDateTime updatedAt, String updatedBy, String profilePictureUrl, String ownerCode, String companyName, String companyEmail,
            String companyNumber, String bannerImageUrl, String facebookUrl, String twitterUrl, String instagramUrl,
            String stripeAccountId, Boolean stripeOnboardingComplete, String subscriptionStatus, LocalDateTime trialEndsAt, 
            LocalDateTime nextBillingDate, UUID currentPlanId, String stripeCustomerId, Boolean autoRenewEnabled) {
        super(userId, fullName, email, phone, passwordHash, role, emailVerified, lastLoginAt, createdAt, createdBy,
                updatedAt, updatedBy, "Active", profilePictureUrl);
        this.ownerCode = ownerCode;
        this.companyName = companyName;
        this.companyEmail = companyEmail;
        this.companyNumber = companyNumber;
        this.bannerImageUrl = bannerImageUrl;
        this.facebookUrl = facebookUrl;
        this.twitterUrl = twitterUrl;
        this.instagramUrl = instagramUrl;
        this.stripeAccountId = stripeAccountId;
        this.stripeOnboardingComplete = stripeOnboardingComplete;
        this.subscriptionStatus = subscriptionStatus;
        this.trialEndsAt = trialEndsAt;
        this.nextBillingDate = nextBillingDate;
        this.currentPlanId = currentPlanId;
        this.stripeCustomerId = stripeCustomerId;
        this.autoRenewEnabled = autoRenewEnabled;
    }

}
