package com.workflowplatform.attachment.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Instant;
import java.util.Date;

/**
 * Storage service backed by MinIO via the AWS S3 SDK.
 *
 * Object key convention: /{tenantId}/{processInstanceId}/{fieldKey}/{filename}
 * This structure enables:
 *   - Per-tenant access isolation via IAM/bucket policies
 *   - Easy lifecycle policies by processInstanceId prefix
 *   - Logical grouping of field attachments
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final AmazonS3 amazonS3;

    @Value("${minio.bucket:workflow-attachments}")
    private String bucket;

    @Value("${minio.presigned-url-expiry-minutes:60}")
    private int presignedUrlExpiryMinutes;

    /**
     * Generate a pre-signed PUT URL for direct browser-to-MinIO upload.
     *
     * @param objectKey   the full S3 object key
     * @param contentType MIME type of the file being uploaded
     * @return pre-signed URL valid for {@code presignedUrlExpiryMinutes}
     */
    public String generatePresignedUploadUrl(String objectKey, String contentType) {
        Date expiration = Date.from(
            Instant.now().plusSeconds((long) presignedUrlExpiryMinutes * 60));

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, objectKey)
            .withMethod(HttpMethod.PUT)
            .withExpiration(expiration)
            .withContentType(contentType);

        URL url = amazonS3.generatePresignedUrl(request);
        log.debug("Generated upload URL for objectKey={} expiry={}min", objectKey, presignedUrlExpiryMinutes);
        return url.toString();
    }

    /**
     * Generate a pre-signed GET URL for temporary download access.
     *
     * @param objectKey the full S3 object key
     * @return pre-signed URL valid for {@code presignedUrlExpiryMinutes}
     */
    public String generatePresignedDownloadUrl(String objectKey) {
        Date expiration = Date.from(
            Instant.now().plusSeconds((long) presignedUrlExpiryMinutes * 60));

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, objectKey)
            .withMethod(HttpMethod.GET)
            .withExpiration(expiration);

        URL url = amazonS3.generatePresignedUrl(request);
        log.debug("Generated download URL for objectKey={} expiry={}min", objectKey, presignedUrlExpiryMinutes);
        return url.toString();
    }

    /**
     * Delete an object from MinIO.
     *
     * @param objectKey the full S3 object key
     */
    public void deleteObject(String objectKey) {
        try {
            amazonS3.deleteObject(new DeleteObjectRequest(bucket, objectKey));
            log.info("Deleted S3 object objectKey={}", objectKey);
        } catch (Exception e) {
            log.error("Failed to delete S3 object objectKey={}", objectKey, e);
            throw e;
        }
    }

    /**
     * Check whether an object actually exists in MinIO.
     * Used to verify upload completion before saving metadata.
     */
    public boolean objectExists(String objectKey) {
        try {
            amazonS3.getObjectMetadata(bucket, objectKey);
            return true;
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    /**
     * Retrieve object metadata (size, content type, etag) without downloading content.
     */
    public ObjectMetadata getObjectMetadata(String objectKey) {
        return amazonS3.getObjectMetadata(bucket, objectKey);
    }

    /**
     * Build the standard object key for a file attachment.
     * Pattern: {tenantId}/{processInstanceId}/{fieldKey}/{sanitizedFilename}
     */
    public static String buildObjectKey(String tenantId,
                                        String processInstanceId,
                                        String fieldKey,
                                        String filename) {
        String sanitized = sanitizeFilename(filename);
        return String.format("%s/%s/%s/%s", tenantId, processInstanceId, fieldKey, sanitized);
    }

    private static String sanitizeFilename(String filename) {
        // Replace path traversal and shell-dangerous characters
        return filename.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }
}
