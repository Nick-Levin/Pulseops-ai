package com.pulseops.evidence.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket:pulseops-evidence}")
    private String bucketName;

    public void uploadFile(String objectKey, InputStream inputStream, long size, String contentType) {
        try {
            log.debug("Uploading file to MinIO: bucket={}, objectKey={}, size={}, contentType={}",
                    bucketName, objectKey, size, contentType);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );

            log.info("File uploaded successfully to MinIO: bucket={}, objectKey={}", bucketName, objectKey);
        } catch (MinioException e) {
            log.error("MinIO error uploading file: bucket={}, objectKey={}", bucketName, objectKey, e);
            throw new RuntimeException("Failed to upload file to MinIO: " + e.getMessage(), e);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error uploading file to MinIO: bucket={}, objectKey={}", bucketName, objectKey, e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    public InputStream downloadFile(String objectKey) {
        try {
            log.debug("Downloading file from MinIO: bucket={}, objectKey={}", bucketName, objectKey);

            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
        } catch (MinioException e) {
            log.error("MinIO error downloading file: bucket={}, objectKey={}", bucketName, objectKey, e);
            throw new RuntimeException("Failed to download file from MinIO: " + e.getMessage(), e);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error downloading file from MinIO: bucket={}, objectKey={}", bucketName, objectKey, e);
            throw new RuntimeException("Failed to download file: " + e.getMessage(), e);
        }
    }
}
