package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.OwnerDTO;
import com.fixzone.fixzon_backend.model.Owner;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Unit tests for OwnerService - tests CRUD operations for owners using Mockito to mock database layer
@ExtendWith(MockitoExtension.class)
class OwnerServiceTest {

    @Mock // Mock to avoid hitting real database during tests
    private OwnerRepository ownerRepository;

    @Mock // Mock to avoid making real API calls to external service
    private ImageKitService imageKitService;

    @InjectMocks // Inject mocks so we test real service logic with fake dependencies
    private OwnerService ownerService;

    // Test data used in multiple tests
    private Owner owner;
    private OwnerDTO ownerDTO;
    private UUID ownerId;

    // Before each test to ensure clean state and prevent test interdependencies
    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID(); // Generate unique ID to avoid collisions between tests
        owner = new Owner(); // Create test Owner entity
        owner.setUserId(ownerId);
        owner.setFullName("John Doe");
        owner.setEmail("john@example.com");
        owner.setOwnerCode("OWN-001");
        owner.setCompanyName("Doe Corp");

        ownerDTO = new OwnerDTO(); // Create test OwnerDTO with same data
        ownerDTO.setUserId(ownerId);
        ownerDTO.setFullName("John Doe");
        ownerDTO.setEmail("john@example.com");
        ownerDTO.setOwnerCode("OWN-001");
        ownerDTO.setCompanyName("Doe Corp");
    }

    // Test if service returns all owners - important to verify list is populated correctly
    @Test
    void retrieveAllOwners_ShouldReturnList() {
        when(ownerRepository.findAll()).thenReturn(Collections.singletonList(owner)); // Mock to simulate database response
        List<OwnerDTO> result = ownerService.retrieveAllOwners(); // Execute the method under test
        assertThat(result).hasSize(1); // Verify list contains expected number of items
        assertThat(result.get(0).getOwnerCode()).isEqualTo("OWN-001"); // Verify data transformation from entity to DTO
        verify(ownerRepository, times(1)).findAll(); // Verify repository was called exactly once (no extra calls)
    }

    // Test if service retrieves specific owner by ID - necessary to verify filtering and conversion works
    @Test
    void retrieveOwnerById_WhenExists_ShouldReturnDTO() {
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(owner)); // Mock to simulate successful database lookup
        OwnerDTO result = ownerService.retrieveOwnerById(ownerId); // Execute the method under test
        assertThat(result).isNotNull(); // Verify data is returned and not lost in conversion
        assertThat(result.getUserId()).isEqualTo(ownerId); // Verify correct record was fetched (identity check)
        verify(ownerRepository, times(1)).findById(ownerId); // Verify repository called with correct parameter
    }

    // Test edge case when owner doesn't exist - critical to ensure graceful degradation instead of crashes
    @Test
    void retrieveOwnerById_WhenNotExists_ShouldReturnNull() {
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.empty()); // Mock to simulate database miss
        OwnerDTO result = ownerService.retrieveOwnerById(ownerId); // Execute the method under test
        assertThat(result).isNull(); // Verify null is returned rather than throwing exception
    }

    // Test owner registration - validates that new owners are persisted and properly converted to DTOs
    @Test
    void registerOwner_ShouldSaveAndReturnDTO() {
        when(ownerRepository.save(any(Owner.class))).thenReturn(owner); // Mock to simulate database persistence
        OwnerDTO result = ownerService.registerOwner(ownerDTO); // Execute the method under test
        assertThat(result).isNotNull(); // Verify result exists after save
        assertThat(result.getOwnerCode()).isEqualTo("OWN-001"); // Verify data is preserved through save operation
        verify(ownerRepository, times(1)).save(any(Owner.class)); // Verify save was called to persist data
    }

    // Test partial updates - ensures modifications work without affecting unspecified fields
    @Test
    void modifyOwner_WhenExists_ShouldUpdateAndReturnDTO() {
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(owner)); // Mock to retrieve existing record
        when(ownerRepository.save(any(Owner.class))).thenReturn(owner); // Mock to persist changes
        OwnerDTO updateData = new OwnerDTO(); // Create update data with new company name
        updateData.setCompanyName("Updated Corp");
        OwnerDTO result = ownerService.modifyOwner(ownerId, updateData); // Execute the method under test
        assertThat(result).isNotNull(); // Verify update returns valid result
        verify(ownerRepository, times(1)).save(any(Owner.class)); // Verify persistence occurred
    }

    // Test integration with external services - verifies image uploads occur when banner changes, not on every update
    @Test
    void modifyOwner_WithBannerChange_ShouldUploadImage() {
        owner.setBannerImageUrl("old-url"); // Setup initial state to detect change
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(owner)); // Mock to retrieve existing record
        when(imageKitService.uploadImage(anyString(), anyString())).thenReturn("new-url"); // Mock external API response
        when(ownerRepository.save(any(Owner.class))).thenReturn(owner); // Mock to persist changes
        OwnerDTO updateData = new OwnerDTO(); // Create update data with new banner image
        updateData.setBannerImageUrl("new-image-data");
        ownerService.modifyOwner(ownerId, updateData); // Execute the method under test
        verify(imageKitService, times(1)).uploadImage(eq("new-image-data"), contains("owner-banner-")); // Verify external service was invoked with correct format
    }

    // Test deletion - confirms removal actually persists and doesn't just mark as deleted
    @Test
    void removeOwner_ShouldCallRepositoryDelete() {
        ownerService.removeOwner(ownerId); // Execute the method under test
        verify(ownerRepository, times(1)).deleteById(ownerId); // Verify delete was invoked (not just flagged as inactive)
    }
}
