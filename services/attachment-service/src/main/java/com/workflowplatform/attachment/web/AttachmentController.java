package com.workflowplatform.attachment.web;

import com.workflowplatform.attachment.dto.AttachmentDto;
import com.workflowplatform.attachment.dto.PresignedDownloadResponseDto;
import com.workflowplatform.attachment.dto.PresignedUploadResponseDto;
import com.workflowplatform.attachment.dto.UploadUrlRequest;
import com.workflowplatform.attachment.service.AttachmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    /**
     * POST /api/v1/attachments/upload-url
     *
     * Request a pre-signed PUT URL for uploading a file directly to MinIO.
     * Returns 201 with the pre-signed URL and a pending attachment ID.
     *
     * Upload workflow:
     *   1. POST here to get the upload URL and attachment ID
     *   2. PUT the file directly to the returned URL
     *   3. POST /{id}/confirm to mark the attachment as confirmed
     */
    @PostMapping("/upload-url")
    public ResponseEntity<PresignedUploadResponseDto> generateUploadUrl(
            @Valid @RequestBody UploadUrlRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId   = jwt.getClaimAsString("tenant_id");
        String uploadedBy = jwt.getSubject();

        PresignedUploadResponseDto response = attachmentService.generateUploadUrl(
            tenantId,
            request.getProcessInstanceId(),
            request.getTaskId(),
            request.getFieldKey(),
            request.getFileName(),
            request.getContentType(),
            request.getFileSize(),
            uploadedBy
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/v1/attachments/{id}/confirm
     *
     * Confirm that the file has been uploaded to MinIO and mark the
     * attachment metadata record as confirmed.
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<AttachmentDto> confirmUpload(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");
        AttachmentDto dto = attachmentService.confirmUpload(id, tenantId);
        return ResponseEntity.ok(dto);
    }

    /**
     * GET /api/v1/attachments/download-url/{id}
     *
     * Get a pre-signed GET URL to download an attachment.
     */
    @GetMapping("/download-url/{id}")
    public ResponseEntity<PresignedDownloadResponseDto> generateDownloadUrl(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");
        PresignedDownloadResponseDto response = attachmentService.generateDownloadUrl(id, tenantId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/attachments
     *
     * List confirmed attachments for a process instance.
     * Optional filters: taskId, fieldKey.
     */
    @GetMapping
    public ResponseEntity<List<AttachmentDto>> listAttachments(
            @RequestParam String processInstanceId,
            @RequestParam(required = false) String taskId,
            @RequestParam(required = false) String fieldKey,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");
        List<AttachmentDto> attachments = attachmentService.listAttachments(
            tenantId, processInstanceId, taskId, fieldKey);
        return ResponseEntity.ok(attachments);
    }

    /**
     * DELETE /api/v1/attachments/{id}
     *
     * Soft-delete an attachment record and remove the object from MinIO.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");
        attachmentService.deleteAttachment(id, tenantId);
        return ResponseEntity.noContent().build();
    }
}
