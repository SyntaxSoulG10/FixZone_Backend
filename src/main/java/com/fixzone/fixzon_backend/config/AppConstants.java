package com.fixzone.fixzon_backend.config;

public final class AppConstants {
    private AppConstants() {} // Prevent instantiation

    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_PENDING = "PENDING";
    
    public static final String PAYMENT_METHOD_CARD = "CARD";
    public static final String PAYMENT_METHOD_ONLINE = "ONLINE";
    public static final String PAYMENT_METHOD_CASH = "CASH";

    public static final String PERIOD_DAILY = "daily";
    public static final String PERIOD_YEARLY = "yearly";

    public static final String DEFAULT_PENDING_EXPECTATION = "+5.0%";

    public static final String ROLE_OWNER = "OWNER";
    public static final String ROLE_MANAGER = "MANAGER";
    
    public static final String OWNER_BANNER_PREFIX = "owner-banner-";
    public static final String OWNER_PROFILE_PREFIX = "owner-profile-";
    
    public static final String ROLE_SERVICE_MANAGER = "ROLE_SERVICE_MANAGER";
    public static final String STATUS_ACTIVE = "Active";
    public static final String MANAGER_PREFIX = "MGR-";
    public static final String CUSTOMER_PREFIX = "CUST-";
    public static final String OWNER_PREFIX = "OWN-";
    public static final String DEFAULT_PASSWORD = "Manager123!";
    
    public static final String DEFAULT_TENANT_ID = "00000000-0000-0000-0000-000000000000";
    public static final String PENALTY_PERCENT_5 = "0.05";
    public static final int RESCHEDULE_MIN_DAYS_LEFT = 3;
    
    public static final int BASE_MECHANICS_COUNT = 5;
    public static final int MECHANICS_VARIANCE_MODULO = 5;
    public static final int BASE_CAPACITY = 40;
    public static final int CAPACITY_VARIANCE_MODULO = 30;
}
