package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.SuperAdminDTO;
import com.fixzone.fixzon_backend.model.SuperAdmin;
import com.fixzone.fixzon_backend.repository.SuperAdminRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SuperAdminService {

    @Autowired
    private SuperAdminRepository superAdminRepository;

    public List<SuperAdminDTO> getAllSuperAdmins() {
        return superAdminRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public SuperAdminDTO getSuperAdminById(UUID id) {
        return superAdminRepository.findById(id)
                .map(this::convertToDTO)
                .orElse(null);
    }

    public SuperAdminDTO createSuperAdmin(SuperAdminDTO superAdminDTO) {
        SuperAdmin superAdmin = convertToEntity(superAdminDTO);
        if (superAdmin.getUserId() == null) {
            superAdmin.setUserId(UUID.randomUUID());
        }
        SuperAdmin saved = superAdminRepository.save(superAdmin);
        return convertToDTO(saved);
    }

    public SuperAdminDTO updateSuperAdmin(UUID id, SuperAdminDTO superAdminDTO) {
        if (superAdminRepository.existsById(id)) {
            SuperAdmin superAdmin = convertToEntity(superAdminDTO);
            superAdmin.setUserId(id);
            SuperAdmin saved = superAdminRepository.save(superAdmin);
            return convertToDTO(saved);
        }
        return null;
    }

    public void deleteSuperAdmin(UUID id) {
        superAdminRepository.deleteById(id);
    }

    private SuperAdminDTO convertToDTO(SuperAdmin superAdmin) {
        SuperAdminDTO dto = new SuperAdminDTO();
        BeanUtils.copyProperties(superAdmin, dto);
        return dto;
    }

    private SuperAdmin convertToEntity(SuperAdminDTO dto) {
        SuperAdmin superAdmin = new SuperAdmin();
        BeanUtils.copyProperties(dto, superAdmin);
        return superAdmin;
    }
}
