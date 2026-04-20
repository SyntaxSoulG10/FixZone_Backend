package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.ManagerDTO;
import com.fixzone.fixzon_backend.model.Manager;
import com.fixzone.fixzon_backend.repository.ManagerRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ManagerService {

    // Dependency injection via constructor keeps strictly initialized references 
    // ensuring the repository cannot be null at runtime.
    private final ManagerRepository managerRepository;

    public ManagerService(ManagerRepository managerRepository) {
        this.managerRepository = managerRepository;
    }

    public List<ManagerDTO> getAllManagers() {
        // Enforce the separation between DB layers and API presentation layers
        // by always mapping outbound entities into pure DTOs.
        return managerRepository.findAll().stream()
                .map(this::transformToDataTransferObject)
                .collect(Collectors.toList());
    }

    public ManagerDTO getManagerById(UUID id) {
        Objects.requireNonNull(id, "ID must not be null");
        return managerRepository.findById(id)
                .map(this::transformToDataTransferObject)
                .orElse(null);
    }

    public ManagerDTO createManager(ManagerDTO managerDTO) {
        Manager manager = transformToDatabaseEntity(managerDTO);
        if (manager.getUserId() == null) {
            manager.setUserId(UUID.randomUUID());
        }
        Manager savedManager = managerRepository.save(manager);
        return transformToDataTransferObject(savedManager);
    }

    public ManagerDTO updateManager(UUID id, ManagerDTO managerDTO) {
        Objects.requireNonNull(id, "ID must not be null");
        if (managerRepository.existsById(id)) {
            Manager manager = transformToDatabaseEntity(managerDTO);
            if (manager != null) {
                manager.setUserId(id);
                Manager savedManager = managerRepository.save(manager);
                return transformToDataTransferObject(savedManager);
            }
        }
        return null;
    }

    public void deleteManager(UUID id) {
        Objects.requireNonNull(id, "ID must not be null");
        managerRepository.deleteById(id);
    }

    // Extracted transformation logic ensures the business layer is decoupled purely 
    // from structural changes to the entity models.
    private ManagerDTO transformToDataTransferObject(Manager manager) {
        if (manager == null) return null;
        ManagerDTO dto = new ManagerDTO();
        BeanUtils.copyProperties(manager, dto);
        return dto;
    }

    private Manager transformToDatabaseEntity(ManagerDTO dto) {
        if (dto == null) return null;
        Manager manager = new Manager();
        BeanUtils.copyProperties(dto, manager);
        return manager;
    }
}
