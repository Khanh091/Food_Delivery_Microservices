package com.khanh.fooddelivery.restaurant_service.storage;

import com.khanh.fooddelivery.restaurant_service.exception.ErrorCode;
import com.khanh.fooddelivery.restaurant_service.storage.exception.FileStorageException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class FileUploadValidator {

    private static final int MAX_FILENAME_LENGTH = 255;

    private final FileStorageProperties properties;

    public String validateAndSanitize(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException(ErrorCode.FILE_EMPTY);
        }
        if (file.getSize() > properties.getMaxFileSize()) {
            throw new FileStorageException(ErrorCode.FILE_TOO_LARGE);
        }

        String contentType = normalize(file.getContentType());
        boolean allowed =
                properties.getAllowedContentTypes().stream()
                        .map(this::normalize)
                        .anyMatch(contentType::equals);
        if (!allowed) {
            throw new FileStorageException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            return "upload";
        }
        if (originalFilename.contains("..")
                || originalFilename.contains("/")
                || originalFilename.contains("\\")) {
            throw new FileStorageException(
                    ErrorCode.COMMON_VALIDATION_ERROR, "Invalid original filename");
        }

        String sanitized =
                originalFilename.trim().replaceAll("[^A-Za-z0-9._ -]", "_").replaceAll("\\s+", " ");
        if (sanitized.isBlank()) {
            sanitized = "upload";
        }
        return sanitized.substring(0, Math.min(sanitized.length(), MAX_FILENAME_LENGTH));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
