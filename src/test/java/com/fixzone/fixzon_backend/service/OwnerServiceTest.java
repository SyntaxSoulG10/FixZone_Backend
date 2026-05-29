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

@ExtendWith(MockitoExtension.class)
class OwnerServiceTest {

    @Mock
    private OwnerRepository ownerRepository;

    @Mock
    private ImageKitService imageKitService;

    @InjectMocks
    private OwnerService ownerService;

    private Owner owner;
    private OwnerDTO ownerDTO;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        owner = new Owner();
        owner.setUserId(ownerId);
        owner.setFullName("John Doe");
        owner.setEmail("john@example.com");
        owner.setOwnerCode("OWN-001");
        owner.setCompanyName("Doe Corp");

        ownerDTO = new OwnerDTO();
        ownerDTO.setUserId(ownerId);
        ownerDTO.setFullName("John Doe");
        ownerDTO.setEmail("john@example.com");
        ownerDTO.setOwnerCode("OWN-001");
        ownerDTO.setCompanyName("Doe Corp");
    }

    @Test
    void retrieveAllOwners_ShouldReturnList() {
        when(ownerRepository.findAll()).thenReturn(Collections.singletonList(owner));

        List<OwnerDTO> result = ownerService.retrieveAllOwners();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOwnerCode()).isEqualTo("OWN-001");
        verify(ownerRepository, times(1)).findAll();
    }

    @Test
    void retrieveOwnerById_WhenExists_ShouldReturnDTO() {
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(owner));

        OwnerDTO result = ownerService.retrieveOwnerById(ownerId);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(ownerId);
        verify(ownerRepository, times(1)).findById(ownerId);
    }

    @Test
    void retrieveOwnerById_WhenNotExists_ShouldReturnNull() {
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.empty());

        OwnerDTO result = ownerService.retrieveOwnerById(ownerId);

        assertThat(result).isNull();
    }

    @Test
    void registerOwner_ShouldSaveAndReturnDTO() {
        when(ownerRepository.save(any(Owner.class))).thenReturn(owner);

        OwnerDTO result = ownerService.registerOwner(ownerDTO);

        assertThat(result).isNotNull();
        assertThat(result.getOwnerCode()).isEqualTo("OWN-001");
        verify(ownerRepository, times(1)).save(any(Owner.class));
    }

    @Test
    void modifyOwner_WhenExists_ShouldUpdateAndReturnDTO() {
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(ownerRepository.save(any(Owner.class))).thenReturn(owner);

        OwnerDTO updateData = new OwnerDTO();
        updateData.setCompanyName("Updated Corp");

        OwnerDTO result = ownerService.modifyOwner(ownerId, updateData);

        assertThat(result).isNotNull();
        verify(ownerRepository, times(1)).save(any(Owner.class));
    }

    @Test
    void modifyOwner_WithBannerChange_ShouldUploadImage() {
        owner.setBannerImageUrl("old-url");
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(imageKitService.uploadImage(anyString(), anyString())).thenReturn("new-url");
        when(ownerRepository.save(any(Owner.class))).thenReturn(owner);

        OwnerDTO updateData = new OwnerDTO();
        updateData.setBannerImageUrl("new-image-data");

        ownerService.modifyOwner(ownerId, updateData);

        verify(imageKitService, times(1)).uploadImage(eq("new-image-data"), contains("owner-banner-"));
    }

    @Test
    void removeOwner_ShouldCallRepositoryDelete() {
        ownerService.removeOwner(ownerId);
        verify(ownerRepository, times(1)).deleteById(ownerId);
    }
}
