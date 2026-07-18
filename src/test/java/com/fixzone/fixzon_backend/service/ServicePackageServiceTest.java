package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.ServicePackageDTO;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.model.ServicePackage;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.ServicePackageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicePackageServiceTest {

    @Mock
    private ServicePackageRepository servicePackageRepository;

    @Mock
    private ServiceCenterRepository serviceCenterRepository;

    @InjectMocks
    private ServicePackageService servicePackageService;

    private UUID packageId;
    private UUID centerId;
    private ServiceCenter center;
    private ServicePackage samplePackage;

    @BeforeEach
    void setUp() {
        packageId = UUID.randomUUID();
        centerId = UUID.randomUUID();

        center = new ServiceCenter();
        center.setCenterId(centerId);
        center.setName("FixZone Colombo");

        samplePackage = new ServicePackage();
        samplePackage.setPackageId(packageId);
        samplePackage.setName("Full Service");
        samplePackage.setDescription("Complete service package");
        samplePackage.setBasePrice(new BigDecimal("10000.00"));
        samplePackage.setIsActive(true);
        samplePackage.setServiceCenter(center);
    }

    @Test
    void getAllPackages_ShouldReturnActiveServicePackageDTOList() {
        when(servicePackageRepository.findByIsActiveTrue()).thenReturn(Collections.singletonList(samplePackage));

        List<ServicePackageDTO> result = servicePackageService.getAllPackages();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Full Service");
        verify(servicePackageRepository, times(1)).findByIsActiveTrue();
    }

    @Test
    void getPackagesByCenter_ShouldReturnServicePackageDTOList() {
        when(servicePackageRepository.findByServiceCenter_CenterIdAndIsActiveTrue(centerId))
                .thenReturn(Collections.singletonList(samplePackage));

        List<ServicePackageDTO> result = servicePackageService.getPackagesByCenter(centerId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCenterId()).isEqualTo(centerId);
        verify(servicePackageRepository, times(1)).findByServiceCenter_CenterIdAndIsActiveTrue(centerId);
    }

    @Test
    void getPackagesByCenter_ShouldThrowException_WhenCenterIdIsNull() {
        assertThatThrownBy(() -> servicePackageService.getPackagesByCenter(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Center ID cannot be null");
    }

    @Test
    void getPackagesByCenterAndVehicleType_ShouldReturnAllPackages_WhenVehicleTypeIsEmpty() {
        when(servicePackageRepository.findByServiceCenter_CenterIdAndIsActiveTrue(centerId))
                .thenReturn(Collections.singletonList(samplePackage));

        List<ServicePackageDTO> result = servicePackageService.getPackagesByCenterAndVehicleType(centerId, "");

        assertThat(result).hasSize(1);
        verify(servicePackageRepository, times(1)).findByServiceCenter_CenterIdAndIsActiveTrue(centerId);
    }

    @Test
    void getPackagesByCenterAndVehicleType_ShouldReturnSpecificPackages_WhenVehicleTypeIsValid() {
        String vehicleType = "CAR";
        when(servicePackageRepository.findByCenterIdAndVehicleType(centerId, vehicleType))
                .thenReturn(Collections.singletonList(samplePackage));

        List<ServicePackageDTO> result = servicePackageService.getPackagesByCenterAndVehicleType(centerId, vehicleType);

        assertThat(result).hasSize(1);
        verify(servicePackageRepository, times(1)).findByCenterIdAndVehicleType(centerId, vehicleType);
    }

    @Test
    void getPackagesByOwnerEmail_ShouldReturnPackages() {
        String email = "owner@fixzone.com";
        when(servicePackageRepository.findPackagesByOwnerEmail(email)).thenReturn(Collections.singletonList(samplePackage));

        List<ServicePackageDTO> result = servicePackageService.getPackagesByOwnerEmail(email);

        assertThat(result).hasSize(1);
        verify(servicePackageRepository, times(1)).findPackagesByOwnerEmail(email);
    }

    @Test
    void getPackagesByOwnerEmail_ShouldThrowException_WhenEmailIsEmpty() {
        assertThatThrownBy(() -> servicePackageService.getPackagesByOwnerEmail("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email cannot be null or empty");
    }

    @Test
    void getPackagesByOwnerCode_ShouldReturnPackages() {
        String code = "COMP123";
        when(servicePackageRepository.findPackagesByOwnerCode(code)).thenReturn(Collections.singletonList(samplePackage));

        List<ServicePackageDTO> result = servicePackageService.getPackagesByOwnerCode(code);

        assertThat(result).hasSize(1);
        verify(servicePackageRepository, times(1)).findPackagesByOwnerCode(code);
    }

    @Test
    void getPackagesByOwnerCode_ShouldThrowException_WhenCodeIsEmpty() {
        assertThatThrownBy(() -> servicePackageService.getPackagesByOwnerCode(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Owner code cannot be null or empty");
    }

    @Test
    void getPackageById_ShouldReturnDTO_WhenFound() {
        when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(samplePackage));

        ServicePackageDTO result = servicePackageService.getPackageById(packageId);

        assertThat(result).isNotNull();
        assertThat(result.getPackageId()).isEqualTo(packageId);
        verify(servicePackageRepository, times(1)).findById(packageId);
    }

    @Test
    void getPackageById_ShouldReturnNull_WhenNotFound() {
        when(servicePackageRepository.findById(packageId)).thenReturn(Optional.empty());

        ServicePackageDTO result = servicePackageService.getPackageById(packageId);

        assertThat(result).isNull();
    }

    @Test
    void getPackageById_ShouldThrowException_WhenIdIsNull() {
        assertThatThrownBy(() -> servicePackageService.getPackageById(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ID must not be null");
    }

    @Test
    void createPackage_ShouldSaveAndReturnDTO() {
        ServicePackageDTO dto = new ServicePackageDTO();
        dto.setName("Full Service");
        dto.setCenterId(centerId);
        dto.setBasePrice(new BigDecimal("10000.00"));
        dto.setIsActive(true);

        when(serviceCenterRepository.findById(centerId)).thenReturn(Optional.of(center));
        when(servicePackageRepository.save(any(ServicePackage.class))).thenReturn(samplePackage);

        ServicePackageDTO result = servicePackageService.createPackage(dto);

        assertThat(result).isNotNull();
        assertThat(result.getPackageId()).isEqualTo(packageId);
        verify(serviceCenterRepository, times(1)).findById(centerId);
        verify(servicePackageRepository, times(1)).save(any(ServicePackage.class));
    }

    @Test
    void createPackage_ShouldThrowException_WhenDTOIsNull() {
        assertThatThrownBy(() -> servicePackageService.createPackage(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Service Package data cannot be null");
    }

    @Test
    void updatePackage_ShouldModifyAndSavePackage() {
        ServicePackageDTO dto = new ServicePackageDTO();
        dto.setName("Premium Service");
        dto.setCenterId(centerId);
        dto.setBasePrice(new BigDecimal("15000.00"));
        dto.setIsActive(true);

        ServicePackage updatedPackage = new ServicePackage();
        updatedPackage.setPackageId(packageId);
        updatedPackage.setName("Premium Service");
        updatedPackage.setBasePrice(new BigDecimal("15000.00"));
        updatedPackage.setIsActive(true);
        updatedPackage.setServiceCenter(center);

        when(servicePackageRepository.findById(packageId)).thenReturn(Optional.of(samplePackage));
        when(serviceCenterRepository.findById(centerId)).thenReturn(Optional.of(center));
        when(servicePackageRepository.save(any(ServicePackage.class))).thenReturn(updatedPackage);

        ServicePackageDTO result = servicePackageService.updatePackage(packageId, dto);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Premium Service");
        verify(servicePackageRepository, times(1)).findById(packageId);
        verify(serviceCenterRepository, times(1)).findById(centerId);
        verify(servicePackageRepository, times(1)).save(any(ServicePackage.class));
    }

    @Test
    void deletePackage_ShouldCallRepositoryDelete_WhenPackageExists() {
        when(servicePackageRepository.existsById(packageId)).thenReturn(true);

        servicePackageService.deletePackage(packageId);

        verify(servicePackageRepository, times(1)).existsById(packageId);
        verify(servicePackageRepository, times(1)).deleteById(packageId);
    }

    @Test
    void deletePackage_ShouldThrowException_WhenPackageDoesNotExist() {
        when(servicePackageRepository.existsById(packageId)).thenReturn(false);

        assertThatThrownBy(() -> servicePackageService.deletePackage(packageId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Service package not found with id");
    }
}
