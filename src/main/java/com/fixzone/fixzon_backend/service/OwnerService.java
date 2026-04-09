package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.DTO.OwnerDTO;
import com.fixzone.fixzon_backend.model.Owner;
import com.fixzone.fixzon_backend.repository.OwnerRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OwnerService {

    @Autowired
    private OwnerRepository ownerRepository;

    public List<OwnerDTO> getAllOwners() {
        return ownerRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public OwnerDTO getOwnerById(UUID id) {
        Objects.requireNonNull(id, "ID must not be null");
        return ownerRepository.findById(id)
                .map(this::convertToDTO)
                .orElse(null);
    }

    public OwnerDTO createOwner(OwnerDTO ownerDTO) {
        Owner owner = convertToEntity(ownerDTO);
        if (owner.getUserId() == null) {
            owner.setUserId(UUID.randomUUID());
        }
        Owner saved = ownerRepository.save(owner);
        return convertToDTO(saved);
    }

    public OwnerDTO updateOwner(UUID id, OwnerDTO ownerDTO) {
        Objects.requireNonNull(id, "ID must not be null");
        if (ownerRepository.existsById(id)) {
            Owner owner = convertToEntity(ownerDTO);
            if (owner != null) {
                owner.setUserId(id);
                Owner saved = ownerRepository.save(owner);
                return convertToDTO(saved);
            }
        }
        return null;
    }

    public void deleteOwner(UUID id) {
        Objects.requireNonNull(id, "ID must not be null");
        ownerRepository.deleteById(id);
    }

    private OwnerDTO convertToDTO(Owner owner) {
        if (owner == null) return null;
        OwnerDTO dto = new OwnerDTO();
        BeanUtils.copyProperties(owner, dto);
        return dto;
    }

    private Owner convertToEntity(OwnerDTO dto) {
        if (dto == null) return null;
        Owner owner = new Owner();
        BeanUtils.copyProperties(dto, owner);
        return owner;
    }
}
