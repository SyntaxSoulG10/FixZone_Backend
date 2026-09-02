package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.SuperAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface SuperAdminRepository extends JpaRepository<SuperAdmin, UUID> {
    Optional<SuperAdmin> findByAdminCode(String adminCode);
    Optional<SuperAdmin> findByEmail(String email);
}
