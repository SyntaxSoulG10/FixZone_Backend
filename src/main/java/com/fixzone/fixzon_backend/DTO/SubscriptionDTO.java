package com.fixzone.fixzon_backend.DTO;

import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class SubscriptionDTO {
    private UUID id;
    private LocalDate startDate;
    private LocalDate endDate;
    private String planType;
    private String status;
    private String billingHistory;
    private UUID ownerId;
    private String ownerName;
    private String companyName;
    private String nextBilling;
}
