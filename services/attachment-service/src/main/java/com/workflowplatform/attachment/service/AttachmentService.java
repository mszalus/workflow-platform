package com.workflowplatform.attachment.service;

import com.workflowplatform.attachment.domain.Attachment;
import com.workflowplatform.attachment.dto.AttachmentDto;
import com.workflowplatform.attachment.dto.PresignedDownloadResponseDto;
import com.workflowplatform.attachment.dto.PresignedUploadResponseDto;
import com.workflowplatform.attachment.repository.AttachmentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final MinioStorageService storageService;

    @Value("${minio.presigned-url-expiry-minutes:60}")
    private int presignedUrlExpiryMinutes;

    /**
     * Generate a pre-signed PUT URL for direct upload to MinIO and create a
     * pending (unconfirmed) Attachment record.
     */
    public PresignedUploadResponseDto generateUploadUrl(
            String tenantId,
            String processInstanceId,
            String taskId,
            String fieldKey,
            String fileName,
            String contentType,
            Long fileSize,
            String uploadedBy) {

        String objectKey = MinioStorageService.buildObjectKey(
            tenantId, processInstanceId, fieldKey, fileName);

        Attachment attachment = Attachment.builder()
            .tenantId(tenantId)
            .processInstanceId(processInstanceId)
            .taskId(taskId)
            .fieldKey(fieldKey)
            .fileName(fileName)
            .contentType(contentType)
            .fileSize(fileSize)
            .objectKey(objectKey)
            .uploadedBy(uploadedBy)
            .confirmed(false)
            .deleted(false)
            .build();

        attachment = attachmentRepository.save(attachment);

        String uploadUrl = storageService.generatePresignedUploadUrl(objectKey, contentType);
        Instant expiresAt = Instant.now().plusSeconds((long) presignedUrlExpiryMinutes * 60);

        log.info("Generated upload URL for attachment id={} objectKey={} tenant={}",
            attachment.getId(), objectKey, tenantId);

        return PresignedUploadResponseDto.builder()
            .attachmentId(attachment.getId())
            .uploadUrl(uploadUrl)
            .expiresAt(expiresAt)
            .build();
    }

    /**
     * Confirm that a file has been successfully uploaded to MinIO.
     * Verifies the object exists and marks the attachment as confirmed.
     */
    public AttachmentDto confirmUpload(UUID attachmentId, String tenantId) {
        Attachment attachment = attachmentRepository
            .findByTenantIdAndIdAndDeletedFalse(tenantId, attachmentId)
            .orElseThrow(() -> new EntityNotFoundException("Attachment not found: " + attachmentId));

        if (!storageService.objectExists(attachment.getObjectKey())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "File has not been uploaded yet for objectKey=" + attachment.getObjectKey());
        }

        // Try to get actual file size from MinIO
        try {
            var metadata = storageService.getObjectMetadata(attachment.getObjectKey());
            attachment.setFileSize(metadata.getContentLength());
        } catch (Exception e) {
            log.warn("Could not retrieve metadata for objectKey={}, keeping client-supplied size",
                attachment.getObjectKey());
        }

        attachment.setConfirmed(true);
        attachment = attachmentRepository.save(attachment);

        log.info("Confirmed attachment id={} objectKey={} tenant={}",
            attachment.getId(), attachment.getObjectKey(), tenantId);

        return toDto(attachment);
    }

    /**
     * Generate a pre-signed GET URL for downloading an attachment.
     */
    @Transactional(readOnly = true)
    public PresignedDownloadResponseDto generateDownloadUrl(UUID attachmentId, String tenantId) {
        Attachment attachment = attachmentRepository
            .findByTenantIdAndIdAndDeletedFalse(tenantId, attachmentId)
            .orElseThrow(() -> new EntityNotFoundException("Attachment not found: " + attachmentId));

        String downloadUrl = storageService.generatePresignedDownloadUrl(attachment.getObjectKey());
        Instant expiresAt  = Instant.now().plusSeconds((long) presignedUrlExpiryMinutes * 60);

        return PresignedDownloadResponseDto.builder()
            .downloadUrl(downloadUrl)
            .fileName(attachment.getFileName())
            .contentType(attachment.getContentType())
            .expiresAt(expiresAt)
            .build();
    }

    /**
     * List attachments for a process instance with optional task and field filters.
     * Only returns confirmed, non-deleted attachments.
     */
    @Transactional(readOnly = true)
    public List<AttachmentDto> listAttachments(
            String tenantId,
            String processInstanceId,
            String taskId,
            String fieldKey) {

        List<Attachment> attachments;
        if (fieldKey != null) {
            attachments = attachmentRepository
                .findByTenantIdAndProcessInstanceIdAndFieldKeyAndDeletedFalse(
                    tenantId, processInstanceId, fieldKey);
        } else if (taskId != null) {
            attachments = attachmentRepository
                .findByTenantIdAndProcessInstanceIdAndTaskIdAndDeletedFalse(
                    tenantId, processInstanceId, taskId);
        } else {
            attachments = attachmentRepository
                .findByTenantIdAndProcessInstanceIdAndDeletedFalse(tenantId, processInstanceId);
        }

        return attachments.stream()
            .filter(Attachment::isConfirmed)
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    /**
     * Soft-delete an attachment and remove the object from MinIO (best effort).
     */
    public void deleteAttachment(UUID id, String tenantId) {
        Attachment attachment = attachmentRepository
            .findByTenantIdAndIdAndDeletedFalse(tenantId, id)
            .orElseThrow(() -> new EntityNotFoundException("Attachment not found: " + id));

        // Best-effort MinIO deletion
        try {
            storageService.deleteObject(attachment.getObjectKey());
        } catch (Exception e) {
            log.error("Failed to delete S3 object objectKey={}, soft-deleting anyway",
                attachment.getObjectKey(), e);
        }

        attachment.setDeleted(true);
        attachment.setDeletedAt(Instant.now());
        attachmentRepository.save(attachment);

        log.info("Soft-deleted attachment id={} objectKey={} tenant={}",
            id, attachment.getObjectKey(), tenantId);
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private AttachmentDto toDto(Attachment a) {
        return AttachmentDto.builder()
            .id(a.getId())
            .tenantId(a.getTenantId())
            .processInstanceId(a.getProcessInstanceId())
            .taskId(a.getTaskId())
            .fieldKey(a.getFieldKey())
            .fileName(a.getFileName())
            .contentType(a.getContentType())
            .fileSize(a.getFileSize())
            .objectKey(a.getObjectKey())
            .uploadedBy(a.getUploadedBy())
            .uploadedAt(a.getUploadedAt())
            .confirmed(a.isConfirmed())
            .build();
    }
}
