package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.CustomerDTO;
import com.fixzone.fixzon_backend.model.Customer;
import com.fixzone.fixzon_backend.model.Owner;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.repository.CustomerRepository;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ServiceCenterRepository serviceCenterRepository;

    @Mock
    private OwnerRepository ownerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer customer;
    private Owner owner;
    private ServiceCenter center;
    private UUID ownerId;
    private UUID centerId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        centerId = UUID.randomUUID();

        customer = new Customer();
        customer.setUserId(UUID.randomUUID());
        customer.setFullName("Jane Doe");
        customer.setEmail("jane@example.com");
        customer.setCustomerCode("CUST-001");
        customer.setVisits(3);
        customer.setTotalSpent(new BigDecimal("5000.00"));

        owner = new Owner();
        owner.setUserId(ownerId);
        owner.setOwnerCode("OWN-001");

        center = new ServiceCenter();
        center.setCenterId(centerId);
        center.setOwner(owner);
    }

    @Test
    void getAllCustomers_ShouldReturnList() {
        when(customerRepository.findAll()).thenReturn(Collections.singletonList(customer));

        List<CustomerDTO> result = customerService.getAllCustomers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerCode()).isEqualTo("CUST-001");
        assertThat(result.get(0).getFullName()).isEqualTo("Jane Doe");
        verify(customerRepository, times(1)).findAll();
    }

    @Test
    void getCustomersByOwnerCode_WhenCodeIsEmpty_ShouldThrowException() {
        assertThatThrownBy(() -> customerService.getCustomersByOwnerCode(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Owner code cannot be null or empty");
    }

    @Test
    void getCustomersByOwnerCode_WhenOwnerNotFound_ShouldReturnEmptyList() {
        when(ownerRepository.findByOwnerCode("INVALID")).thenReturn(Optional.empty());

        List<CustomerDTO> result = customerService.getCustomersByOwnerCode("INVALID");

        assertThat(result).isEmpty();
        verify(ownerRepository, times(1)).findByOwnerCode("INVALID");
    }

    @Test
    void getCustomersByOwnerCode_WhenOwnerFoundAndHasCenters_ShouldReturnCustomersList() {
        when(ownerRepository.findByOwnerCode("OWN-001")).thenReturn(Optional.of(owner));
        when(serviceCenterRepository.findByOwner_UserId(ownerId)).thenReturn(Collections.singletonList(center));
        when(customerRepository.findCustomersByCenterIds(Collections.singletonList(centerId))).thenReturn(Collections.singletonList(customer));

        List<CustomerDTO> result = customerService.getCustomersByOwnerCode("OWN-001");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerCode()).isEqualTo("CUST-001");
        verify(ownerRepository, times(1)).findByOwnerCode("OWN-001");
        verify(serviceCenterRepository, times(1)).findByOwner_UserId(ownerId);
        verify(customerRepository, times(1)).findCustomersByCenterIds(Collections.singletonList(centerId));
    }
}
