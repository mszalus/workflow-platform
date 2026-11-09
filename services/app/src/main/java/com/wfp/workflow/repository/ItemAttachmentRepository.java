package com.wfp.workflow.repository;

import com.wfp.workflow.dto.AttachmentDto;
import com.wfp.workflow.entity.ItemAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemAttachmentRepository extends JpaRepository<ItemAttachment, UUID> {

    @Query("select new com.wfp.workflow.dto.AttachmentDto(a.id, a.fileName, a.sizeBytes, a.uploadedBy, a.uploadedAt) "
            + "from ItemAttachment a where a.itemId = :itemId and a.tenantId = :tenantId order by a.uploadedAt desc")
    List<AttachmentDto> findMetadataByItem(UUID itemId, String tenantId);

    Optional<ItemAttachment> findByIdAndItemIdAndTenantId(UUID id, UUID itemId, String tenantId);
}
