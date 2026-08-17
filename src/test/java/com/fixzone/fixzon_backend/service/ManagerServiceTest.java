package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.ManagerDTO;
import com.fixzone.fixzon_backend.model.Manager;
import com.fixzone.fixzon_backend.model.Owner;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.repository.AuthRepository;
import com.fixzone.fixzon_backend.repository.ManagerRepository;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ManagerServiceTest {

    @Mock private ManagerRepository managerRepository;
    @Mock private ServiceCenterRepository serviceCenterRepository;
    @Mock private OwnerRepository ownerRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private AuthRepository authRepository;

    private ManagerService managerService;

    @BeforeEach
    void setUp() {
        managerService = new ManagerService(
                managerRepository,
                serviceCenterRepository,
                ownerRepository,
                passwordEncoder,
                emailService,
                authRepository
        );
    }

    @Test
    void testCreateManagerGeneratesUniquePasswordAndEmailsCredentials() {
        ManagerDTO dto = new ManagerDTO();
        dto.setEmail("manager@gmail.com");
        dto.setFullName("John Doe");
        UUID centerId = UUID.randomUUID();
        dto.setManagedCenterId(centerId);
        dto.setSendInvite(true);

        when(authRepository.findByEmailIgnoreCase("manager@gmail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "encoded_" + inv.getArgument(0));
        when(managerRepository.save(any(Manager.class))).thenAnswer(inv -> inv.getArgument(0));

        ServiceCenter center = new ServiceCenter();
        center.setCenterId(centerId);
        center.setName("Colombo Branch");
        Owner owner = new Owner();
        owner.setUserId(UUID.randomUUID());
        owner.setCompanyName("AutoFix Ltd");
        center.setOwner(owner);

        when(serviceCenterRepository.findById(centerId)).thenReturn(Optional.of(center));
        when(ownerRepository.findById(owner.getUserId())).thenReturn(Optional.of(owner));

        ManagerDTO result = managerService.createManager(dto);

        assertNotNull(result);
        assertEquals("manager@gmail.com", result.getEmail());

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendManagerCredentialsEmail(
                eq("manager@gmail.com"),
                eq("John Doe"),
                passwordCaptor.capture(),
                eq("Colombo Branch"),
                eq("AutoFix Ltd")
        );

        String sentPassword = passwordCaptor.getValue();
        assertNotNull(sentPassword);
        assertTrue(sentPassword.length() >= 8);
    }

    @Test
    void testResendInvitationGeneratesNewUniquePassword() {
        UUID managerId = UUID.randomUUID();
        Manager manager = new Manager();
        manager.setUserId(managerId);
        manager.setEmail("alex@fixzone.com");
        manager.setFullName("Alex Morgan");
        manager.setStatus("INVITED");

        when(managerRepository.findById(managerId)).thenReturn(Optional.of(manager));
        when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "encoded_" + inv.getArgument(0));
        when(managerRepository.save(any(Manager.class))).thenAnswer(inv -> inv.getArgument(0));

        managerService.resendInvitation(managerId);

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendManagerCredentialsEmail(
                eq("alex@fixzone.com"),
                eq("Alex Morgan"),
                passwordCaptor.capture(),
                isNull(),
                isNull()
        );

        String newPassword = passwordCaptor.getValue();
        assertNotNull(newPassword);
        assertTrue(newPassword.length() >= 8);
        assertEquals("encoded_" + newPassword, manager.getPasswordHash());
    }
}
