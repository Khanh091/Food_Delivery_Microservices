package com.khanh.fooddelivery.catalog_service.storage;

import com.khanh.fooddelivery.catalog_service.exception.AppException;
import com.khanh.fooddelivery.catalog_service.exception.ErrorCode;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class ImageUploadValidator {
    private final StorageProperties properties;

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > properties.getMaxFileSize()) {
            throw new AppException(ErrorCode.INVALID_IMAGE_FILE);
        }

        String contentType = normalize(file.getContentType());
        boolean allowed =
                properties.getAllowedContentTypes().stream()
                        .map(this::normalize)
                        .anyMatch(contentType::equals);
        if (!allowed) {
            throw new AppException(ErrorCode.INVALID_IMAGE_FILE);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
