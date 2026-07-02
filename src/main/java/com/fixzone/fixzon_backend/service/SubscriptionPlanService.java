package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.model.SubscriptionPlan;
import com.fixzone.fixzon_backend.repository.SubscriptionPlanRepository;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@SuppressWarnings("null")
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository planRepository;
    private final OwnerRepository ownerRepository;
    private final NotificationService notificationService;

    public SubscriptionPlanService(SubscriptionPlanRepository planRepository, OwnerRepository ownerRepository, NotificationService notificationService) {
        this.planRepository = planRepository;
        this.ownerRepository = ownerRepository;
        this.notificationService = notificationService;
    }

    public List<SubscriptionPlan> getAllPlans() {
        return planRepository.findAll();
    }

    public SubscriptionPlan getPlanById(UUID id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription Plan not found with id: " + id));
    }

    @Transactional
    public SubscriptionPlan createPlan(SubscriptionPlan plan) {
        if (planRepository.existsByName(plan.getName())) {
            throw new RuntimeException("Plan with name " + plan.getName() + " already exists");
        }
        SubscriptionPlan savedPlan = planRepository.save(plan);

        // Notify all owners
        String message = "A new subscription plan '" + savedPlan.getName() + "' is now available for Rs. " + savedPlan.getPrice() + "/mo. Click to review options.";
        notificationService.broadcastNotificationSafe(ownerRepository.findAll(), "New Subscription Plan Available", message, "SUCCESS", "/dashboard/company-owner");

        return savedPlan;
    }

    @Transactional
    public SubscriptionPlan updatePlan(UUID id, SubscriptionPlan planDetails) {
        SubscriptionPlan plan = getPlanById(id);
        
        plan.setName(planDetails.getName());
        plan.setPrice(planDetails.getPrice());
        plan.setDescription(planDetails.getDescription());
        plan.setDurationMonths(planDetails.getDurationMonths());
        plan.setIsActive(planDetails.getIsActive());
        plan.setFeatures(planDetails.getFeatures());
        plan.setIsPopular(planDetails.getIsPopular());
        
        SubscriptionPlan savedPlan = planRepository.save(plan);

        // Notify all owners
        String message = "Subscription plan '" + savedPlan.getName() + "' has been updated (Price: Rs. " + savedPlan.getPrice() + "/mo). Click to review.";
        notificationService.broadcastNotificationSafe(ownerRepository.findAll(), "Subscription Plan Updated", message, "INFO", "/dashboard/company-owner");

        return savedPlan;
    }

    @Transactional
    public void deletePlan(UUID id) {
        SubscriptionPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan not found with id: " + id));
        String planName = plan.getName();
        planRepository.delete(plan);

        // Notify all owners
        String message = "Subscription plan '" + planName + "' is no longer available. Click to review pricing options.";
        notificationService.broadcastNotificationSafe(ownerRepository.findAll(), "Subscription Plan Removed", message, "WARNING", "/dashboard/company-owner");
    }
}
