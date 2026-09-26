package com.wfp.notification.repository;

import com.wfp.notification.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {
    List<NotificationPreference> findByUserIdAndTenantId(String userId, String tenantId);
    Optional<NotificationPreference> findByUserIdAndTenantIdAndEventType(
            String userId, String tenantId, String eventType);
}
