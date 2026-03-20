package com.wfp.workflow.repository;

import com.wfp.workflow.entity.ProcessMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProcessMetadataRepository extends JpaRepository<ProcessMetadata, UUID> {
    Optional<ProcessMetadata> findByProcessDefinitionKeyAndTenantId(String key, String tenantId);
}
