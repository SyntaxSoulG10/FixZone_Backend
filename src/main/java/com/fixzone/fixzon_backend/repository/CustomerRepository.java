package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;

import java.time.LocalDateTime;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    @org.springframework.data.jpa.repository.Query("SELECT c FROM Customer c WHERE c.userId IN (SELECT b.customerId FROM Booking b WHERE b.centerId IN :centerIds)")
    List<Customer> findCustomersByCenterIds(@org.springframework.data.repository.query.Param("centerIds") List<UUID> centerIds);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    java.util.Optional<Customer> findByEmail(String email);
}
