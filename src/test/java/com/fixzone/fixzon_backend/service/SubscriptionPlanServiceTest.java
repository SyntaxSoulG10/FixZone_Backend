package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.model.SubscriptionPlan;
import com.fixzone.fixzon_backend.repository.SubscriptionPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubscriptionPlanServiceTest {

    @Mock
    private SubscriptionPlanRepository planRepository;

    @InjectMocks
    private SubscriptionPlanService planService;

    private SubscriptionPlan samplePlan;
    private UUID planId;

    @BeforeEach
    void setUp() {
        planId = UUID.randomUUID();
        samplePlan = new SubscriptionPlan();
        samplePlan.setId(planId);
        samplePlan.setName("Gold Plan");
        samplePlan.setPrice(new BigDecimal("99.99"));
        samplePlan.setFeatures(Arrays.asList("Feature 1", "Feature 2"));
    }

    @Test
    void getAllPlans_ShouldReturnList() {
        // Arrange
        when(planRepository.findAll()).thenReturn(Arrays.asList(samplePlan));

        // Act
        List<SubscriptionPlan> result = planService.getAllPlans();

        // Assert
        assertEquals(1, result.size());
        assertEquals("Gold Plan", result.get(0).getName());
        verify(planRepository, times(1)).findAll();
    }

    @Test
    void getPlanById_PlanExists_ShouldReturnPlan() {
        // Arrange
        when(planRepository.findById(planId)).thenReturn(Optional.of(samplePlan));

        // Act
        SubscriptionPlan result = planService.getPlanById(planId);

        // Assert
        assertNotNull(result);
        assertEquals(planId, result.getId());
    }

    @Test
    void createPlan_NameAlreadyExists_ShouldThrowException() {
        // Arrange
        when(planRepository.existsByName("Gold Plan")).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            planService.createPlan(samplePlan);
        });

        assertEquals("Plan with name Gold Plan already exists", exception.getMessage());
        verify(planRepository, never()).save(any());
    }

    @Test
    void createPlan_NewName_ShouldSavePlan() {
        // Arrange
        when(planRepository.existsByName("Gold Plan")).thenReturn(false);
        when(planRepository.save(any(SubscriptionPlan.class))).thenReturn(samplePlan);

        // Act
        SubscriptionPlan result = planService.createPlan(samplePlan);

        // Assert
        assertNotNull(result);
        assertEquals("Gold Plan", result.getName());
        verify(planRepository).save(samplePlan);
    }

    @Test
    void deletePlan_Exists_ShouldCallDelete() {
        // Arrange
        when(planRepository.existsById(planId)).thenReturn(true);

        // Act
        planService.deletePlan(planId);

        // Assert
        verify(planRepository).deleteById(planId);
    }
}
