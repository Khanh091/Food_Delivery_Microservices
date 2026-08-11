package com.khanh.fooddelivery.restaurant_service.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.khanh.fooddelivery.restaurant_service.exception.ErrorCode;
import com.khanh.fooddelivery.restaurant_service.storage.exception.FileStorageException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class FileUploadValidatorTests {

    private FileStorageProperties properties;
    private FileUploadValidator validator;

    @BeforeEach
    void setUp() {
        properties = new FileStorageProperties();
        properties.setMaxFileSize(10);
        validator = new FileUploadValidator(properties);
    }

    @Test
    void acceptsAllowedFileAndSanitizesFilename() {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "giay phép.pdf",
                        "application/pdf",
                        "valid".getBytes(StandardCharsets.UTF_8));

        assertThat(validator.validateAndSanitize(file)).isEqualTo("giay ph_p.pdf");
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file =
                new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        assertStorageError(file, ErrorCode.FILE_EMPTY);
    }

    @Test
    void rejectsOversizedFile() {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "large.pdf",
                        "application/pdf",
                        new byte
                                [properties.getMaxFileSize() > Integer.MAX_VALUE
                                        ? Integer.MAX_VALUE
                                        : (int) properties.getMaxFileSize() + 1]);

        assertStorageError(file, ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    void rejectsUnsupportedContentType() {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "document.exe",
                        "application/octet-stream",
                        "content".getBytes(StandardCharsets.UTF_8));

        assertStorageError(file, ErrorCode.FILE_TYPE_NOT_ALLOWED);
    }

    @Test
    void rejectsPathTraversalFilename() {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "../document.pdf",
                        "application/pdf",
                        "content".getBytes(StandardCharsets.UTF_8));

        assertStorageError(file, ErrorCode.COMMON_VALIDATION_ERROR);
    }

    private void assertStorageError(MockMultipartFile file, ErrorCode expectedErrorCode) {
        assertThatThrownBy(() -> validator.validateAndSanitize(file))
                .isInstanceOf(FileStorageException.class)
                .extracting("errorCode")
                .isEqualTo(expectedErrorCode);
    }
}
