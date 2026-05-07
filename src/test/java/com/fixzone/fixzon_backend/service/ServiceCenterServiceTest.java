package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.ServiceCenterDTO;
import com.fixzone.fixzon_backend.model.Owner;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Unit tests for ServiceCenterService - tests CRUD operations for service centers using mock repositories
@ExtendWith(MockitoExtension.class)
class ServiceCenterServiceTest {

    @Mock private ServiceCenterRepository serviceCenterRepository; // Mock to avoid database queries
    @Mock private UserRepository userRepository; // Mock to avoid user lookups from database
    @Mock private ServicePackageRepository servicePackageRepository; // Mock to avoid package queries
    @Mock private OwnerRepository ownerRepository; // Mock to avoid owner lookups
    @Mock private InvoiceRepository invoiceRepository; // Mock to avoid invoice aggregations
    @Mock private ManagerRepository managerRepository; // Mock to avoid manager lookups

    @InjectMocks // Inject mocks to test real service with fake dependencies
    private ServiceCenterService serviceCenterService;

    // Test data used in multiple tests
    private ServiceCenter center;
    private UUID centerId;
    private UUID ownerId;

    // Before each test to ensure isolated test execution
    @BeforeEach
    void setUp() {
        centerId = UUID.randomUUID(); // Generate unique center ID to prevent test collision
        ownerId = UUID.randomUUID(); // Generate unique owner ID to prevent test collision
        
        Owner owner = new Owner(); // Create test owner
        owner.setUserId(ownerId);

        center = new ServiceCenter(); // Create test service center
        center.setCenterId(centerId);
        center.setName("Test Center");
        center.setOwner(owner);
        center.setIsActive(true);
    }

    // Test retrieval of all centers - important to verify only active centers are returned with proper data enrichment
    @Test
    void getAllServiceCenters_ShouldReturnList() {
        when(serviceCenterRepository.findByIsActive(true)).thenReturn(Collections.singletonList(center)); // Mock to simulate filtering active only
        when(invoiceRepository.sumTotalByCenterIdIn(anyList())).thenReturn(new ArrayList<>()); // Mock to handle bulk revenue calculation
        when(servicePackageRepository.findByServiceCenter_CenterIdInAndIsActiveTrue(anyList())).thenReturn(new ArrayList<>()); // Mock to handle bulk package fetching
        when(managerRepository.findByManagedCenterIdIn(anyList())).thenReturn(new ArrayList<>()); // Mock to handle bulk manager fetching
        List<ServiceCenterDTO> result = serviceCenterService.getAllServiceCenters(); // Execute the method under test
        assertThat(result).hasSize(1); // Verify correct number of results
        assertThat(result.get(0).getName()).isEqualTo("Test Center"); // Verify data mapping from entity to DTO
    }

    // Test single center retrieval with enriched data - validates proper data aggregation and conversion
    @Test
    void getServiceCenterById_WhenExists_ShouldReturnDTO() {
        when(serviceCenterRepository.findById(centerId)).thenReturn(Optional.of(center)); // Mock to simulate center lookup
        when(servicePackageRepository.findByServiceCenter_CenterIdAndIsActiveTrue(centerId)).thenReturn(new ArrayList<>()); // Mock to get associated packages
        when(invoiceRepository.sumTotalByCenterId(centerId)).thenReturn(BigDecimal.ZERO); // Mock to calculate revenue
        when(managerRepository.findByManagedCenterId(centerId)).thenReturn(new ArrayList<>()); // Mock to get associated managers
        ServiceCenterDTO result = serviceCenterService.getServiceCenterById(centerId); // Execute the method under test
        assertThat(result).isNotNull(); // Verify enriched data is returned
        assertThat(result.getCenterId()).isEqualTo(centerId); // Verify correct center is fetched
    }

    // Test service center creation - ensures owner validation happens before persistence
    @Test
    void createServiceCenter_ShouldSaveAndReturnDTO() {
        ServiceCenterDTO dto = new ServiceCenterDTO(); // Create new center DTO with data
        dto.setName("New Center");
        dto.setOwnerId(ownerId);
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(new Owner())); // Mock to validate owner exists before creating center
        when(serviceCenterRepository.save(any(ServiceCenter.class))).thenReturn(center); // Mock to simulate center persistence
        ServiceCenterDTO result = serviceCenterService.createServiceCenter(dto); // Execute the method under test
        assertThat(result).isNotNull(); // Verify creation returns valid result
        verify(serviceCenterRepository, times(1)).save(any(ServiceCenter.class)); // Verify persistence actually occurred
    }

    // Test cascading delete - critical to ensure all related data is cleaned up to prevent orphaned records
    @Test
    void deleteServiceCenter_ShouldPerformCascadingCleanup() {
        when(servicePackageRepository.findByServiceCenter_CenterId(centerId)).thenReturn(new ArrayList<>()); // Mock to get packages before delete
        when(managerRepository.findByManagedCenterId(centerId)).thenReturn(new ArrayList<>()); // Mock to get managers before delete
        when(invoiceRepository.findByCenterId(centerId)).thenReturn(new ArrayList<>()); // Mock to get invoices before delete
        serviceCenterService.deleteServiceCenter(centerId); // Execute the method under test
        verify(serviceCenterRepository, times(1)).deleteById(centerId); // Verify center record is removed
        verify(servicePackageRepository, times(1)).deleteAll(anyList()); // Verify all associated packages are also deleted
    }
}
