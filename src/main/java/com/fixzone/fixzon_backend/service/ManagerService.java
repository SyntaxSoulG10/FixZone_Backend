package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.ManagerDTO;
import com.fixzone.fixzon_backend.model.Manager;
import com.fixzone.fixzon_backend.repository.ManagerRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ManagerService {

    @Autowired
    private ManagerRepository managerRepository;

    public List<ManagerDTO> getAllManagers() {
        return managerRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ManagerDTO getManagerById(UUID id) {
        Objects.requireNonNull(id, "ID must not be null");
        return managerRepository.findById(id)
                .map(this::convertToDTO)
                .orElse(null);
    }

    public ManagerDTO createManager(ManagerDTO managerDTO) {
        Manager manager = convertToEntity(managerDTO);
        if (manager.getUserId() == null) {
            manager.setUserId(UUID.randomUUID());
        }
        Manager savedManager = managerRepository.save(manager);
        return convertToDTO(savedManager);
    }

    public ManagerDTO updateManager(UUID id, ManagerDTO managerDTO) {
        Objects.requireNonNull(id, "ID must not be null");
        if (managerRepository.existsById(id)) {
            Manager manager = convertToEntity(managerDTO);
            if (manager != null) {
                manager.setUserId(id);
                Manager savedManager = managerRepository.save(manager);
                return convertToDTO(savedManager);
            }
        }
        return null;
    }

    public void deleteManager(UUID id) {
        Objects.requireNonNull(id, "ID must not be null");
        managerRepository.deleteById(id);
    }

    private ManagerDTO convertToDTO(Manager manager) {
        if (manager == null) return null;
        ManagerDTO dto = new ManagerDTO();
        BeanUtils.copyProperties(manager, dto);
        return dto;
    }

    private Manager convertToEntity(ManagerDTO dto) {
        if (dto == null) return null;
        Manager manager = new Manager();
        BeanUtils.copyProperties(dto, manager);
        return manager;
    }
}
