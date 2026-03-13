package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.entity.User;
import com.fixzone.fixzon_backend.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByRole(Role role);
    long countByRole(Role role);
}
