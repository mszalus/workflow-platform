package com.workflowplatform.schema.repository;

import com.workflowplatform.schema.domain.FieldSchema;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FieldSchemaRepository extends JpaRepository<FieldSchema, UUID> {

    Page<FieldSchema> findByTenantId(String tenantId, Pageable pageable);

    Page<FieldSchema> findByTenantIdAndProcessDefKey(String tenantId, String processDefKey, Pageable pageable);

    Optional<FieldSchema> findByTenantIdAndProcessDefKeyAndIsActiveTrue(String tenantId, String processDefKey);

    Optional<FieldSchema> findByTenantIdAndIdAndIsActiveTrue(String tenantId, UUID id);

    Optional<FieldSchema> findByTenantIdAndProcessDefKeyAndVersion(String tenantId, String processDefKey, int version);

    boolean existsByTenantIdAndId(String tenantId, UUID id);

    @Query("SELECT MAX(fs.version) FROM FieldSchema fs WHERE fs.tenantId = :tenantId AND fs.processDefKey = :processDefKey")
    Optional<Integer> findMaxVersionByTenantIdAndProcessDefKey(
        @Param("tenantId") String tenantId,
        @Param("processDefKey") String processDefKey
    );

    @Modifying
    @Query("UPDATE FieldSchema fs SET fs.isActive = false WHERE fs.tenantId = :tenantId AND fs.processDefKey = :processDefKey AND fs.id != :excludeId")
    int deactivateOtherVersions(
        @Param("tenantId") String tenantId,
        @Param("processDefKey") String processDefKey,
        @Param("excludeId") UUID excludeId
    );
}
