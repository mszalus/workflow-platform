package com.workflowplatform.attachment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadUrlRequest {

    @NotBlank(message = "processInstanceId is required")
    private String processInstanceId;

    /**
     * Optional task ID when the upload is associated with a specific user task.
     */
    private String taskId;

    @NotBlank(message = "fieldKey is required")
    private String fieldKey;

    @NotBlank(message = "fileName is required")
    private String fileName;

    private String contentType;

    private Long fileSize;
}
