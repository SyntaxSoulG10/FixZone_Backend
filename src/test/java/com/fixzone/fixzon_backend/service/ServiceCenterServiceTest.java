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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.fixzone.fixzon_backend.DTO.PagedResponse;

@ExtendWith(MockitoExtension.class)
class ServiceCenterServiceTest {

    @Mock private ServiceCenterRepository serviceCenterRepository;
    @Mock private UserRepository userRepository;
    @Mock private ServicePackageRepository servicePackageRepository;
    @Mock private OwnerRepository ownerRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private ManagerRepository managerRepository;

    @InjectMocks
    private ServiceCenterService serviceCenterService;

    private ServiceCenter center;
    private UUID centerId;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        centerId = UUID.randomUUID();
        ownerId = UUID.randomUUID();

        Owner owner = new Owner();
        owner.setUserId(ownerId);

        center = new ServiceCenter();
        center.setCenterId(centerId);
        center.setName("Test Center");
        center.setOwner(owner);
        center.setIsActive(true);
    }

    @Test
    void getAllServiceCenters_ShouldReturnList() {
        Page<ServiceCenter> page = new PageImpl<>(Collections.singletonList(center));
        when(serviceCenterRepository.findActiveAndValidSubscription(any(Pageable.class))).thenReturn(page);
        // Mocking bulk data fetching used in mapEntitiesToDtos
        when(invoiceRepository.sumTotalByCenterIdIn(anyList())).thenReturn(new ArrayList<>());
        when(servicePackageRepository.findByServiceCenter_CenterIdInAndIsActiveTrue(anyList())).thenReturn(new ArrayList<>());
        when(managerRepository.findByManagedCenterIdIn(anyList())).thenReturn(new ArrayList<>());

        PagedResponse<ServiceCenterDTO> result = serviceCenterService.getAllServiceCenters(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Test Center");
    }

    @Test
    void getServiceCenterById_WhenExists_ShouldReturnDTO() {
        when(serviceCenterRepository.findById(centerId)).thenReturn(Optional.of(center));
        when(servicePackageRepository.findByServiceCenter_CenterIdAndIsActiveTrue(centerId)).thenReturn(new ArrayList<>());
        when(invoiceRepository.sumTotalByCenterId(centerId)).thenReturn(BigDecimal.ZERO);
        when(managerRepository.findByManagedCenterId(centerId)).thenReturn(new ArrayList<>());

        ServiceCenterDTO result = serviceCenterService.getServiceCenterById(centerId);

        assertThat(result).isNotNull();
        assertThat(result.getCenterId()).isEqualTo(centerId);
    }

    @Test
    void createServiceCenter_ShouldSaveAndReturnDTO() {
        ServiceCenterDTO dto = new ServiceCenterDTO();
        dto.setName("New Center");
        dto.setOwnerId(ownerId);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(new Owner()));
        when(serviceCenterRepository.save(any(ServiceCenter.class))).thenReturn(center);

        ServiceCenterDTO result = serviceCenterService.createServiceCenter(dto);

        assertThat(result).isNotNull();
        verify(serviceCenterRepository, times(1)).save(any(ServiceCenter.class));
    }

    @Test
    void deleteServiceCenter_ShouldPerformCascadingCleanup() {
        when(serviceCenterRepository.existsById(centerId)).thenReturn(true);
        when(servicePackageRepository.findByServiceCenter_CenterId(centerId)).thenReturn(new ArrayList<>());
        when(managerRepository.findByManagedCenterId(centerId)).thenReturn(new ArrayList<>());
        when(invoiceRepository.findByCenterId(centerId)).thenReturn(new ArrayList<>());

        serviceCenterService.deleteServiceCenter(centerId);

        verify(serviceCenterRepository, times(1)).deleteById(centerId);
        verify(servicePackageRepository, times(1)).deleteAll(anyList());
    }
}
