package com.wfp.notification.repository;

import com.wfp.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByUserIdAndTenantIdOrderByCreatedAtDesc(String userId, String tenantId, Pageable pageable);
    long countByUserIdAndTenantIdAndReadFalse(String userId, String tenantId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.id IN :ids AND n.tenantId = :tenantId")
    void markAsRead(List<UUID> ids, String tenantId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.userId = :userId AND n.tenantId = :tenantId")
    void markAllAsRead(String userId, String tenantId);
}
