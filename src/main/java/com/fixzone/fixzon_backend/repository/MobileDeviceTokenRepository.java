package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.MobileDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MobileDeviceTokenRepository extends JpaRepository<MobileDeviceToken, UUID> {

    Optional<MobileDeviceToken> findByToken(String token);

    List<MobileDeviceToken> findByUserIdAndActiveTrue(UUID userId);

    List<MobileDeviceToken> findByUserIdInAndActiveTrue(List<UUID> userIds);
}
