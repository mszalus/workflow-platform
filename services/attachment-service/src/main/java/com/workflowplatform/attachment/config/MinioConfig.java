package com.workflowplatform.attachment.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${minio.access-key:minioadmin}")
    private String accessKey;

    @Value("${minio.secret-key:minioadmin}")
    private String secretKey;

    @Value("${minio.bucket:workflow-attachments}")
    private String bucket;

    /**
     * AmazonS3 client configured to use MinIO as the S3-compatible backend.
     * Path-style access is required for MinIO.
     */
    @Bean
    public AmazonS3 amazonS3() {
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
            .withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration(endpoint, "us-east-1"))
            .withCredentials(new AWSStaticCredentialsProvider(credentials))
            .withPathStyleAccessEnabled(true)   // Required for MinIO
            .build();

        // Ensure the bucket exists at startup
        if (!s3Client.doesBucketExistV2(bucket)) {
            log.info("Creating MinIO bucket: {}", bucket);
            s3Client.createBucket(bucket);
        }

        return s3Client;
    }
}
