package com.workflowplatform.notification.repository;

import com.workflowplatform.notification.domain.Notification;
import com.workflowplatform.notification.domain.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByTenantIdAndRecipientIdOrderByCreatedAtDesc(
        String tenantId, String recipientId, Pageable pageable);

    Page<Notification> findByTenantIdAndRecipientIdAndIsReadFalseOrderByCreatedAtDesc(
        String tenantId, String recipientId, Pageable pageable);

    long countByTenantIdAndRecipientIdAndIsReadFalse(String tenantId, String recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt " +
           "WHERE n.tenantId = :tenantId AND n.recipientId = :recipientId AND n.isRead = false")
    int markAllAsRead(
        @Param("tenantId") String tenantId,
        @Param("recipientId") String recipientId,
        @Param("readAt") Instant readAt
    );

    boolean existsBySourceEventId(String sourceEventId);
}
