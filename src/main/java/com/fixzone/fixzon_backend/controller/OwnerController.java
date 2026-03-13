package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.OwnerDTO;
import com.fixzone.fixzon_backend.service.OwnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/owners")
public class OwnerController {

    @Autowired
    private OwnerService ownerService;

    @GetMapping
    public List<OwnerDTO> getAllOwners() {
        return ownerService.getAllOwners();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OwnerDTO> getOwnerById(@PathVariable UUID id) {
        OwnerDTO owner = ownerService.getOwnerById(id);
        return owner != null ? ResponseEntity.ok(owner) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public OwnerDTO createOwner(@RequestBody OwnerDTO ownerDTO) {
        return ownerService.createOwner(ownerDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OwnerDTO> updateOwner(@PathVariable UUID id, @RequestBody OwnerDTO ownerDTO) {
        OwnerDTO updated = ownerService.updateOwner(id, ownerDTO);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOwner(@PathVariable UUID id) {
        ownerService.deleteOwner(id);
        return ResponseEntity.noContent().build();
    }
}
