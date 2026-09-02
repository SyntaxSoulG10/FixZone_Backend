package com.fixzone.fixzon_backend.repository;
 
import com.fixzone.fixzon_backend.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
 
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(UUID recipientId);
    List<Notification> findByRecipientUserIdAndIsArchivedFalseOrderByCreatedAtDesc(UUID recipientId);
    List<Notification> findByRecipientUserIdAndIsArchivedTrueOrderByCreatedAtDesc(UUID recipientId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.recipient.userId = :recipientId")
    void deleteByRecipientUserId(@Param("recipientId") UUID recipientId);
}
