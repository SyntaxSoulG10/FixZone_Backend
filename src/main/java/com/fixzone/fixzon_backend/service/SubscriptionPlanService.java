package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.model.SubscriptionPlan;
import com.fixzone.fixzon_backend.repository.SubscriptionPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository planRepository;

    public SubscriptionPlanService(SubscriptionPlanRepository planRepository) {
        this.planRepository = planRepository;
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
        return planRepository.save(plan);
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
        
        return planRepository.save(plan);
    }

    @Transactional
    public void deletePlan(UUID id) {
        if (!planRepository.existsById(id)) {
            throw new RuntimeException("Plan not found with id: " + id);
        }
        planRepository.deleteById(id);
    }
}
