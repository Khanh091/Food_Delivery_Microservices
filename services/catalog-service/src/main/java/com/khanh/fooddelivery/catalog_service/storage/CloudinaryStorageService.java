package com.khanh.fooddelivery.catalog_service.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.khanh.fooddelivery.catalog_service.exception.AppException;
import com.khanh.fooddelivery.catalog_service.exception.ErrorCode;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
public class CloudinaryStorageService implements StorageService {
    private static final String KEY_SEPARATOR = ":";

    private final StorageProperties properties;

    @Override
    public StorageUploadResult upload(MultipartFile file, String folder, String resourceName) {
        try {
            Map<?, ?> result =
                    configuredClient()
                            .uploader()
                            .upload(
                                    file.getBytes(),
                                    ObjectUtils.asMap(
                                            "folder",
                                            folder,
                                            "public_id",
                                            resourceName,
                                            "resource_type",
                                            "image",
                                            "overwrite",
                                            false,
                                            "use_filename",
                                            false));
            String publicId = required(result, "public_id");
            String resourceType = required(result, "resource_type");
            return new StorageUploadResult(
                    StorageProvider.CLOUDINARY,
                    resourceType + KEY_SEPARATOR + publicId,
                    required(result, "url"),
                    required(result, "secure_url"));
        } catch (IOException | RuntimeException exception) {
            log.error("Cloudinary upload failed for folder {}", folder, exception);
            throw new AppException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }

    @Override
    public void delete(String storageKey) {
        StorageKey parsedKey = parseStorageKey(storageKey);
        try {
            configuredClient()
                    .uploader()
                    .destroy(
                            parsedKey.publicId(),
                            ObjectUtils.asMap(
                                    "resource_type", parsedKey.resourceType(), "invalidate", true));
        } catch (IOException | RuntimeException exception) {
            log.error("Cloudinary delete failed for storage key {}", storageKey, exception);
            throw new AppException(ErrorCode.IMAGE_DELETE_FAILED);
        }
    }

    @Override
    public StorageProvider getProvider() {
        return StorageProvider.CLOUDINARY;
    }

    private Cloudinary configuredClient() {
        StorageProperties.Cloudinary configuration = properties.getCloudinary();
        if (isBlank(configuration.getCloudName())
                || isBlank(configuration.getApiKey())
                || isBlank(configuration.getApiSecret())) {
            throw new AppException(ErrorCode.IMAGE_UPLOAD_FAILED);
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
            throw new AppException(ErrorCode.IMAGE_DELETE_FAILED);
        }
        String[] parts = storageKey.split(KEY_SEPARATOR, 2);
        return new StorageKey(parts[0], parts[1]);
    }

    private String required(Map<?, ?> result, String key) {
        Object value = result.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new AppException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
        return value.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record StorageKey(String resourceType, String publicId) {}
}
