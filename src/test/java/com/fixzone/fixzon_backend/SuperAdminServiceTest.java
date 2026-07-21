package com.fixzone.fixzon_backend;

import com.fixzone.fixzon_backend.DTO.SuperAdminDTO;
import com.fixzone.fixzon_backend.model.SuperAdmin;
import com.fixzone.fixzon_backend.repository.SuperAdminRepository;
import com.fixzone.fixzon_backend.service.SuperAdminService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * =========================================================
 *  Unit Tests – SuperAdminService (CRUD + Profile)
 *  Section: 7.2.x  Super Admin Profile Management
 * =========================================================
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SuperAdminService – CRUD Unit Tests")
class SuperAdminServiceTest {

    @Mock private SuperAdminRepository superAdminRepository;

    @InjectMocks
    private SuperAdminService superAdminService;

    // ─────────────────────────────────────────────────────────────
    //  SECTION 8 – SuperAdmin Profile / CRUD
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-BE-41: getAllSuperAdmins returns all mapped DTOs")
    void getAllSuperAdmins_returnsAll() {
        SuperAdmin admin = createSuperAdmin(UUID.randomUUID(), "admin@fixzone.com");
        when(superAdminRepository.findAll()).thenReturn(List.of(admin));

        List<SuperAdminDTO> result = superAdminService.getAllSuperAdmins();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("admin@fixzone.com");
    }

    @Test
    @DisplayName("TC-BE-42: getSuperAdminById returns correct DTO for valid ID")
    void getSuperAdminById_returnsDTO() {
        UUID id = UUID.randomUUID();
        SuperAdmin admin = createSuperAdmin(id, "sa@fixzone.com");
        when(superAdminRepository.findById(id)).thenReturn(Optional.of(admin));

        SuperAdminDTO result = superAdminService.getSuperAdminById(id);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("sa@fixzone.com");
    }

    @Test
    @DisplayName("TC-BE-43: getSuperAdminById returns null when not found")
    void getSuperAdminById_returnsNull_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(superAdminRepository.findById(id)).thenReturn(Optional.empty());

        SuperAdminDTO result = superAdminService.getSuperAdminById(id);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("TC-BE-44: getSuperAdminById throws NullPointerException for null id")
    void getSuperAdminById_nullId_throwsNPE() {
        assertThatThrownBy(() -> superAdminService.getSuperAdminById(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("TC-BE-45: getSuperAdminByEmail returns correct DTO")
    void getSuperAdminByEmail_returnsDTO() {
        String email = "admin@fixzone.com";
        SuperAdmin admin = createSuperAdmin(UUID.randomUUID(), email);
        when(superAdminRepository.findByEmail(email)).thenReturn(Optional.of(admin));

        SuperAdminDTO result = superAdminService.getSuperAdminByEmail(email);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
    }

    @Test
    @DisplayName("TC-BE-46: getSuperAdminByEmail returns null for unknown email")
    void getSuperAdminByEmail_returnsNull_unknownEmail() {
        when(superAdminRepository.findByEmail("unknown@fixzone.com")).thenReturn(Optional.empty());

        SuperAdminDTO result = superAdminService.getSuperAdminByEmail("unknown@fixzone.com");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("TC-BE-47: createSuperAdmin assigns UUID and returns saved DTO")
    void createSuperAdmin_savesAndReturns() {
        SuperAdminDTO dto = new SuperAdminDTO();
        dto.setEmail("new@fixzone.com");

        SuperAdmin toSave = new SuperAdmin();
        toSave.setUserId(UUID.randomUUID());
        toSave.setEmail("new@fixzone.com");

        when(superAdminRepository.save(any())).thenReturn(toSave);

        SuperAdminDTO result = superAdminService.createSuperAdmin(dto);

        assertThat(result.getEmail()).isEqualTo("new@fixzone.com");
        verify(superAdminRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("TC-BE-48: updateSuperAdmin returns updated DTO when ID exists")
    void updateSuperAdmin_updatesAndReturns() {
        UUID id = UUID.randomUUID();
        SuperAdminDTO dto = new SuperAdminDTO();
        dto.setEmail("updated@fixzone.com");

        SuperAdmin updated = new SuperAdmin();
        updated.setUserId(id);
        updated.setEmail("updated@fixzone.com");

        when(superAdminRepository.existsById(id)).thenReturn(true);
        when(superAdminRepository.save(any())).thenReturn(updated);

        SuperAdminDTO result = superAdminService.updateSuperAdmin(id, dto);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("updated@fixzone.com");
    }

    @Test
    @DisplayName("TC-BE-49: updateSuperAdmin returns null when ID does not exist")
    void updateSuperAdmin_returnsNull_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(superAdminRepository.existsById(id)).thenReturn(false);

        SuperAdminDTO result = superAdminService.updateSuperAdmin(id, new SuperAdminDTO());

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("TC-BE-50: deleteSuperAdmin calls repository deleteById")
    void deleteSuperAdmin_callsRepository() {
        UUID id = UUID.randomUUID();
        doNothing().when(superAdminRepository).deleteById(id);

        superAdminService.deleteSuperAdmin(id);

        verify(superAdminRepository, times(1)).deleteById(id);
    }

    // ─── Helper ───

    private SuperAdmin createSuperAdmin(UUID id, String email) {
        SuperAdmin admin = new SuperAdmin();
        admin.setUserId(id);
        admin.setEmail(email);
        return admin;
    }
}
