package com.wfp.audit.repository;

import com.wfp.audit.entity.AuditEntry;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class AuditEntrySpecification {

    private AuditEntrySpecification() {}

    public static Specification<AuditEntry> withTenant(String tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    public static Specification<AuditEntry> withEntityType(String entityType) {
        return (root, query, cb) -> entityType == null ? null : cb.equal(root.get("entityType"), entityType);
    }

    public static Specification<AuditEntry> withEntityId(String entityId) {
        return (root, query, cb) -> entityId == null ? null : cb.equal(root.get("entityId"), entityId);
    }

    public static Specification<AuditEntry> withUserId(String userId) {
        return (root, query, cb) -> userId == null ? null : cb.equal(root.get("userId"), userId);
    }

    public static Specification<AuditEntry> withEventType(String eventType) {
        return (root, query, cb) -> eventType == null ? null : cb.equal(root.get("eventType"), eventType);
    }

    public static Specification<AuditEntry> afterTimestamp(Instant from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("timestamp"), from);
    }

    public static Specification<AuditEntry> beforeTimestamp(Instant to) {
        return (root, query, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("timestamp"), to);
    }
}
