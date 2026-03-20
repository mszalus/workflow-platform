package com.workflowplatform.attachment.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "attachment",
    indexes = {
        @Index(name = "idx_attachment_tenant_proc", columnList = "tenant_id, process_instance_id"),
        @Index(name = "idx_attachment_tenant_task", columnList = "tenant_id, task_id"),
        @Index(name = "idx_attachment_field_key", columnList = "process_instance_id, field_key"),
        @Index(name = "idx_attachment_object_key", columnList = "object_key", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "process_instance_id", nullable = false, length = 255)
    private String processInstanceId;

    @Column(name = "task_id", length = 255)
    private String taskId;

    @Column(name = "field_key", length = 100)
    private String fieldKey;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "content_type", length = 255)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    /**
     * S3 object key: /{tenantId}/{processInstanceId}/{fieldKey}/{filename}
     */
    @Column(name = "object_key", nullable = false, length = 1000, unique = true)
    private String objectKey;

    @Column(name = "uploaded_by", nullable = false, length = 255)
    private String uploadedBy;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    /**
     * True once the client has confirmed a successful PUT to the pre-signed URL.
     */
    @Column(name = "confirmed", nullable = false)
    @Builder.Default
    private boolean confirmed = false;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
