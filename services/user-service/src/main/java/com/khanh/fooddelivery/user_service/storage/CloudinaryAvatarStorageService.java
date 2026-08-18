package com.khanh.fooddelivery.user_service.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.khanh.fooddelivery.user_service.exception.AppException;
import com.khanh.fooddelivery.user_service.exception.ErrorCode;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CloudinaryAvatarStorageService implements AvatarStorageService {
    private final AvatarStorageProperties properties;

    public CloudinaryAvatarStorageService(AvatarStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public AvatarUploadResult upload(MultipartFile file, String ownerId) {
        validate(file);
        try {
            Map<?, ?> response = configuredClient().uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", normalizedFolder() + "/" + ownerId,
                    "public_id", UUID.randomUUID().toString(), "resource_type", "image",
                    "overwrite", false, "use_filename", false));
            return new AvatarUploadResult("image:" + required(response, "public_id"), required(response, "secure_url"));
        } catch (IOException exception) {
            throw new AppException(ErrorCode.AVATAR_UPLOAD_FAILED, "Avatar upload failed");
        } catch (RuntimeException exception) {
            if (exception instanceof AppException appException) throw appException;
            throw new AppException(ErrorCode.AVATAR_UPLOAD_FAILED, "Avatar upload failed");
        }
    }

    @Override
    public void delete(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) return;
        String[] parts = storageKey.split(":", 2);
        if (parts.length != 2 || parts[1].isBlank()) return;
        try {
            configuredClient().uploader().destroy(parts[1], ObjectUtils.asMap("resource_type", "image", "invalidate", true));
        } catch (IOException exception) {
            throw new AppException(ErrorCode.AVATAR_DELETE_FAILED, "Avatar deletion failed");
        } catch (RuntimeException exception) {
            if (exception instanceof AppException appException) throw appException;
            throw new AppException(ErrorCode.AVATAR_DELETE_FAILED, "Avatar deletion failed");
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new AppException(ErrorCode.AVATAR_FILE_INVALID, "Avatar file is required");
        if (file.getSize() > properties.getMaxFileSize()) throw new AppException(ErrorCode.AVATAR_FILE_TOO_LARGE, "Avatar file is too large");
        String type = file.getContentType() == null ? "" : file.getContentType().trim().toLowerCase(Locale.ROOT);
        if (properties.getAllowedContentTypes().stream().map(value -> value.toLowerCase(Locale.ROOT)).noneMatch(type::equals)) {
            throw new AppException(ErrorCode.AVATAR_FILE_INVALID, "Unsupported avatar file type");
        }
    }

    private Cloudinary configuredClient() {
        AvatarStorageProperties.Cloudinary config = properties.getCloudinary();
        if (blank(config.getCloudName()) || blank(config.getApiKey()) || blank(config.getApiSecret())) {
            throw new AppException(ErrorCode.AVATAR_STORAGE_NOT_CONFIGURED, "Avatar storage is not configured");
        }
        return new Cloudinary(ObjectUtils.asMap("cloud_name", config.getCloudName(), "api_key", config.getApiKey(), "api_secret", config.getApiSecret(), "secure", true));
    }

    private String normalizedFolder() {
        String folder = properties.getCloudinary().getBaseFolder();
        return folder == null ? "food-delivery/avatars" : folder.replace('\\', '/').replaceAll("^/+|/+$", "");
    }

    private String required(Map<?, ?> response, String key) {
        Object value = response.get(key);
        if (value == null || value.toString().isBlank()) throw new AppException(ErrorCode.AVATAR_UPLOAD_FAILED, "Storage response is incomplete");
        return value.toString();
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
