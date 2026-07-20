package com.khanh.fooddelivery.restaurant_service.storage.impl;

import com.khanh.fooddelivery.restaurant_service.exception.ErrorCode;
import com.khanh.fooddelivery.restaurant_service.storage.FileStorageProperties;
import com.khanh.fooddelivery.restaurant_service.storage.FileStorageService;
import com.khanh.fooddelivery.restaurant_service.storage.StorageProvider;
import com.khanh.fooddelivery.restaurant_service.storage.StorageUploadResult;
import com.khanh.fooddelivery.restaurant_service.storage.exception.FileStorageException;
import java.io.IOException;
import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "S3")
public class S3StorageService implements FileStorageService {

    private final FileStorageProperties properties;
    private volatile S3Client s3Client;

    public S3StorageService(FileStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public StorageUploadResult upload(MultipartFile file, String folder, String resourceName) {
        FileStorageProperties.S3 configuration = configuredProperties();
        String key = normalizePath(folder) + "/" + resourceName;
        try {
            client().putObject(
                            PutObjectRequest.builder()
                                    .bucket(configuration.getBucket())
                                    .key(key)
                                    .contentType(file.getContentType())
                                    .contentLength(file.getSize())
                                    .build(),
                            RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            String url =
                    client().utilities()
                            .getUrl(
                                    GetUrlRequest.builder()
                                            .bucket(configuration.getBucket())
                                            .key(key)
                                            .build())
                            .toExternalForm();
            return new StorageUploadResult(
                    StorageProvider.S3,
                    key,
                    url,
                    url,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize());
        } catch (IOException | S3Exception exception) {
            throw new FileStorageException(ErrorCode.FILE_UPLOAD_FAILED, "S3 upload failed");
        }
    }

    @Override
    public void delete(String storageKey) {
        FileStorageProperties.S3 configuration = configuredProperties();
        try {
            client().deleteObject(
                            DeleteObjectRequest.builder()
                                    .bucket(configuration.getBucket())
                                    .key(storageKey)
                                    .build());
        } catch (S3Exception exception) {
            throw new FileStorageException(ErrorCode.FILE_DELETE_FAILED, "S3 delete failed");
        }
    }

    @Override
    public StorageProvider getProvider() {
        return StorageProvider.S3;
    }

    private S3Client client() {
        S3Client currentClient = s3Client;
        if (currentClient != null) {
            return currentClient;
        }
        synchronized (this) {
            if (s3Client == null) {
                s3Client = buildClient(configuredProperties());
            }
            return s3Client;
        }
    }

    private S3Client buildClient(FileStorageProperties.S3 configuration) {
        S3ClientBuilder builder =
                S3Client.builder()
                        .region(resolveRegion(configuration))
                        .serviceConfiguration(
                                S3Configuration.builder()
                                        .pathStyleAccessEnabled(configuration.isPathStyleAccess())
                                        .build());
        if (!isBlank(configuration.getEndpoint())) {
            builder.endpointOverride(URI.create(configuration.getEndpoint()));
        }
        if (!isBlank(configuration.getAccessKey()) && !isBlank(configuration.getSecretKey())) {
            builder.credentialsProvider(
                    StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(
                                    configuration.getAccessKey(), configuration.getSecretKey())));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return builder.build();
    }

    private Region resolveRegion(FileStorageProperties.S3 configuration) {
        if (!isBlank(configuration.getRegion())) {
            return Region.of(configuration.getRegion());
        }
        try {
            return DefaultAwsRegionProviderChain.builder().build().getRegion();
        } catch (RuntimeException exception) {
            throw new FileStorageException(
                    ErrorCode.FILE_STORAGE_NOT_CONFIGURED, "S3 region is not configured");
        }
    }

    private FileStorageProperties.S3 configuredProperties() {
        FileStorageProperties.S3 configuration = properties.getS3();
        if (isBlank(configuration.getBucket())) {
            throw new FileStorageException(
                    ErrorCode.FILE_STORAGE_NOT_CONFIGURED, "S3 bucket is not configured");
        }
        return configuration;
    }

    private String normalizePath(String path) {
        return path.replace('\\', '/').replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
