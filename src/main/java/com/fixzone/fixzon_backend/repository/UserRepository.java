package com.fixzone.fixzon_backend.repository;
 
import com.fixzone.fixzon_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
 
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByRole(String role);
    long countByRole(String role);
}

