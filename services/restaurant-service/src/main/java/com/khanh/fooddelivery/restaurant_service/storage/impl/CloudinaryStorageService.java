package com.khanh.fooddelivery.restaurant_service.storage.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.khanh.fooddelivery.restaurant_service.exception.ErrorCode;
import com.khanh.fooddelivery.restaurant_service.storage.FileStorageProperties;
import com.khanh.fooddelivery.restaurant_service.storage.FileStorageService;
import com.khanh.fooddelivery.restaurant_service.storage.StorageProvider;
import com.khanh.fooddelivery.restaurant_service.storage.StorageUploadResult;
import com.khanh.fooddelivery.restaurant_service.storage.exception.FileStorageException;
import java.io.IOException;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(
        prefix = "app.storage",
        name = "provider",
        havingValue = "CLOUDINARY",
        matchIfMissing = true)
public class CloudinaryStorageService implements FileStorageService {

    private static final String KEY_SEPARATOR = ":";

    private final FileStorageProperties properties;

    public CloudinaryStorageService(FileStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public StorageUploadResult upload(MultipartFile file, String folder, String resourceName) {
        Cloudinary cloudinary = configuredClient();
        try {
            Map<?, ?> result =
                    cloudinary
                            .uploader()
                            .upload(
                                    file.getBytes(),
                                    ObjectUtils.asMap(
                                            "folder",
                                            folder,
                                            "public_id",
                                            resourceName,
                                            "resource_type",
                                            "auto",
                                            "overwrite",
                                            false,
                                            "use_filename",
                                            false));
            String publicId = required(result, "public_id");
            String resourceType = required(result, "resource_type");
            String url = required(result, "url");
            String secureUrl = required(result, "secure_url");
            return new StorageUploadResult(
                    StorageProvider.CLOUDINARY,
                    resourceType + KEY_SEPARATOR + publicId,
                    url,
                    secureUrl,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize());
        } catch (IOException | RuntimeException exception) {
            throw new FileStorageException(
                    ErrorCode.FILE_UPLOAD_FAILED, "Cloudinary upload failed");
        }
    }

    @Override
    public void delete(String storageKey) {
        Cloudinary cloudinary = configuredClient();
        StorageKey parsedKey = parseStorageKey(storageKey);
        try {
            cloudinary
                    .uploader()
                    .destroy(
                            parsedKey.publicId(),
                            ObjectUtils.asMap(
                                    "resource_type", parsedKey.resourceType(), "invalidate", true));
        } catch (IOException | RuntimeException exception) {
            throw new FileStorageException(
                    ErrorCode.FILE_DELETE_FAILED, "Cloudinary delete failed");
        }
    }

    @Override
    public StorageProvider getProvider() {
        return StorageProvider.CLOUDINARY;
    }

    private Cloudinary configuredClient() {
        FileStorageProperties.Cloudinary configuration = properties.getCloudinary();
        if (isBlank(configuration.getCloudName())
                || isBlank(configuration.getApiKey())
                || isBlank(configuration.getApiSecret())) {
            throw new FileStorageException(
                    ErrorCode.FILE_STORAGE_NOT_CONFIGURED,
                    "Cloudinary credentials are not configured");
        }
        return new Cloudinary(
                ObjectUtils.asMap(
                        "cloud_name",
                        configuration.getCloudName(),
                        "api_key",
                        configuration.getApiKey(),
                        "api_secret",
                        configuration.getApiSecret(),
                        "secure",
                        true));
    }

    private StorageKey parseStorageKey(String storageKey) {
        if (isBlank(storageKey) || !storageKey.contains(KEY_SEPARATOR)) {
            throw new FileStorageException(
                    ErrorCode.FILE_DELETE_FAILED, "Invalid Cloudinary storage key");
        }
        String[] parts = storageKey.split(KEY_SEPARATOR, 2);
        return new StorageKey(parts[0], parts[1]);
    }

    private String required(Map<?, ?> result, String key) {
        Object value = result.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new FileStorageException(
                    ErrorCode.FILE_UPLOAD_FAILED, "Cloudinary response is missing " + key);
        }
        return value.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record StorageKey(String resourceType, String publicId) {}
}
