package com.wfp.workflow.repository;

import com.wfp.workflow.entity.FieldSchema;
import com.wfp.workflow.entity.ItemType;
import com.wfp.workflow.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FieldSchemaRepository extends JpaRepository<FieldSchema, UUID> {
    List<FieldSchema> findByProjectOrderBySortOrder(Project project);
    List<FieldSchema> findByProjectAndFieldKey(Project project, String fieldKey);

    @Query("select s from FieldSchema s where s.project = :project "
            + "and (s.itemTypes is empty or :type member of s.itemTypes) order by s.sortOrder")
    List<FieldSchema> findApplicableTo(Project project, ItemType type);
    Optional<FieldSchema> findByIdAndTenantId(UUID id, String tenantId);
}
