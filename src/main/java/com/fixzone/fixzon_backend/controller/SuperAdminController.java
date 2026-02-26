package com.fixzone.fixzon_backend.controller;

import com.fixzone.fixzon_backend.DTO.SuperAdminDTO;
import com.fixzone.fixzon_backend.service.SuperAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/super-admins")
@CrossOrigin(origins = "*")
public class SuperAdminController {

    @Autowired
    private SuperAdminService superAdminService;

    @GetMapping
    public List<SuperAdminDTO> getAllSuperAdmins() {
        return superAdminService.getAllSuperAdmins();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuperAdminDTO> getSuperAdminById(@PathVariable UUID id) {
        SuperAdminDTO superAdmin = superAdminService.getSuperAdminById(id);
        return superAdmin != null ? ResponseEntity.ok(superAdmin) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public SuperAdminDTO createSuperAdmin(@RequestBody SuperAdminDTO superAdminDTO) {
        return superAdminService.createSuperAdmin(superAdminDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SuperAdminDTO> updateSuperAdmin(@PathVariable UUID id,
            @RequestBody SuperAdminDTO superAdminDTO) {
        SuperAdminDTO updated = superAdminService.updateSuperAdmin(id, superAdminDTO);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSuperAdmin(@PathVariable UUID id) {
        superAdminService.deleteSuperAdmin(id);
        return ResponseEntity.noContent().build();
    }
}
