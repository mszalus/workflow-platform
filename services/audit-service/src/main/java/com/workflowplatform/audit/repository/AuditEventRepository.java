package com.workflowplatform.audit.repository;

import com.workflowplatform.audit.domain.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID>,
        JpaSpecificationExecutor<AuditEvent> {

    boolean existsByEventId(String eventId);

    Page<AuditEvent> findByTenantIdOrderByOccurredAtDesc(String tenantId, Pageable pageable);

    @Query("""
        SELECT a FROM AuditEvent a
        WHERE a.tenantId = :tenantId
          AND (:eventType IS NULL OR a.eventType = :eventType)
          AND (:actorId IS NULL OR a.actorId = :actorId)
          AND (:from IS NULL OR a.occurredAt >= :from)
          AND (:to IS NULL OR a.occurredAt <= :to)
        ORDER BY a.occurredAt DESC
        """)
    Page<AuditEvent> findWithFilters(
        @Param("tenantId") String tenantId,
        @Param("eventType") String eventType,
        @Param("actorId") String actorId,
        @Param("from") Instant from,
        @Param("to") Instant to,
        Pageable pageable
    );
}
