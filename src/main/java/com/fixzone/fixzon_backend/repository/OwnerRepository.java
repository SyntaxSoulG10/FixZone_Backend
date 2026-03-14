package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.Optional;

@Repository
public interface OwnerRepository extends JpaRepository<Owner, UUID> {
    Optional<Owner> findByOwnerCode(String ownerCode);
}
