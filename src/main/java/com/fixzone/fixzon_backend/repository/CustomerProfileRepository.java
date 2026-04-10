package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerProfileRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByCustomerId(Long customerId);
}
