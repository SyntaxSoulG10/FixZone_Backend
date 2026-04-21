package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findFirstByBookingIdOrderByIdDesc(Long bookingId);
    Optional<Payment> findByStripeSessionId(String stripeSessionId);
}
