package com.workflowplatform.notification.repository;

import com.workflowplatform.notification.domain.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, String> {

    Optional<NotificationPreference> findByUserIdAndTenantId(String userId, String tenantId);
}
