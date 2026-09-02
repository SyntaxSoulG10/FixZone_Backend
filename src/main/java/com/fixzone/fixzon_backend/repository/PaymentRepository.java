package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findFirstByBookingIdOrderByIdDesc(Long bookingId);
    Optional<Payment> findByGatewaySessionId(String gatewaySessionId);
}
