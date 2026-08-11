package com.khanh.fooddelivery.restaurant_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.restaurant_service.dto.request.ApplicationDocumentUploadRequest;
import com.khanh.fooddelivery.restaurant_service.dto.response.ApplicationDocumentResponse;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantApplicationDocument;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantPartnerApplication;
import com.khanh.fooddelivery.restaurant_service.enums.ApplicationDocumentType;
import com.khanh.fooddelivery.restaurant_service.enums.DocumentVerificationStatus;
import com.khanh.fooddelivery.restaurant_service.enums.PartnerApplicationStatus;
import com.khanh.fooddelivery.restaurant_service.exception.AppException;
import com.khanh.fooddelivery.restaurant_service.exception.ErrorCode;
import com.khanh.fooddelivery.restaurant_service.mapper.ApplicationDocumentMapper;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantApplicationDocumentRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantPartnerApplicationRepository;
import com.khanh.fooddelivery.restaurant_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.restaurant_service.storage.FileStorageProperties;
import com.khanh.fooddelivery.restaurant_service.storage.FileStorageService;
import com.khanh.fooddelivery.restaurant_service.storage.FileUploadValidator;
import com.khanh.fooddelivery.restaurant_service.storage.StorageProvider;
import com.khanh.fooddelivery.restaurant_service.storage.StorageUploadResult;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class ApplicationDocumentServiceImplTests {

    private static final UUID APPLICATION_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String STORAGE_KEY = "raw:documents/key";

    @Mock private RestaurantPartnerApplicationRepository applicationRepository;
    @Mock private RestaurantApplicationDocumentRepository documentRepository;
    @Mock private ApplicationDocumentMapper documentMapper;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private FileStorageService fileStorageService;
    @Mock private FileUploadValidator fileUploadValidator;
    @Mock private Jwt jwt;

    private ApplicationDocumentServiceImpl service;
    private MockMultipartFile file;
    private ApplicationDocumentUploadRequest request;

    @BeforeEach
    void setUp() {
        FileStorageProperties properties = new FileStorageProperties();
        service =
                new ApplicationDocumentServiceImpl(
                        applicationRepository,
                        documentRepository,
                        documentMapper,
                        currentUserProvider,
                        fileStorageService,
                        properties,
                        fileUploadValidator);
        file =
                new MockMultipartFile(
                        "file",
                        "license.pdf",
                        "application/pdf",
                        "document".getBytes(StandardCharsets.UTF_8));
        request =
                new ApplicationDocumentUploadRequest(
                        ApplicationDocumentType.BUSINESS_LICENSE,
                        "LICENSE-123",
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2027, 1, 1));
    }

    @Test
    void storesCompleteMetadataAfterSuccessfulUpload() {
        RestaurantPartnerApplication application = editableApplication();
        StorageUploadResult uploadResult = uploadResult();
        ApplicationDocumentResponse expectedResponse =
                new ApplicationDocumentResponse(
                        UUID.randomUUID(),
                        APPLICATION_ID,
                        ApplicationDocumentType.BUSINESS_LICENSE,
                        "LICENSE-123",
                        "license.pdf",
                        "https://storage.example/license.pdf",
                        "application/pdf",
                        file.getSize(),
                        DocumentVerificationStatus.PENDING,
                        request.issuedAt(),
                        request.expiresAt(),
                        null,
                        null);

        prepareOwnedApplication(application);
        when(fileUploadValidator.validateAndSanitize(file)).thenReturn("license.pdf");
        when(fileStorageService.getProvider()).thenReturn(StorageProvider.CLOUDINARY);
        when(fileStorageService.upload(any(), any(), any())).thenReturn(uploadResult);
        when(documentRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(documentMapper.toResponse(any())).thenReturn(expectedResponse);

        assertThat(service.upload(jwt, APPLICATION_ID, file, request)).isSameAs(expectedResponse);

        ArgumentCaptor<RestaurantApplicationDocument> documentCaptor =
                ArgumentCaptor.forClass(RestaurantApplicationDocument.class);
        verify(documentRepository).saveAndFlush(documentCaptor.capture());
        RestaurantApplicationDocument savedDocument = documentCaptor.getValue();
        assertThat(savedDocument.getStorageProvider()).isEqualTo(StorageProvider.CLOUDINARY);
        assertThat(savedDocument.getStorageKey()).isEqualTo(STORAGE_KEY);
        assertThat(savedDocument.getFileName()).isEqualTo("license.pdf");
        assertThat(savedDocument.getMimeType()).isEqualTo("application/pdf");
        assertThat(savedDocument.getFileSize()).isEqualTo(file.getSize());
        verify(fileStorageService, never()).delete(any());
    }

    @Test
    void deletesUploadedObjectWhenDatabaseSaveFails() {
        RestaurantPartnerApplication application = editableApplication();
        DataIntegrityViolationException databaseException =
                new DataIntegrityViolationException("save failed");

        prepareOwnedApplication(application);
        when(fileUploadValidator.validateAndSanitize(file)).thenReturn("license.pdf");
        when(fileStorageService.getProvider()).thenReturn(StorageProvider.CLOUDINARY);
        when(fileStorageService.upload(any(), any(), any())).thenReturn(uploadResult());
        when(documentRepository.saveAndFlush(any())).thenThrow(databaseException);

        assertThatThrownBy(() -> service.upload(jwt, APPLICATION_ID, file, request))
                .isSameAs(databaseException);
        verify(fileStorageService).delete(STORAGE_KEY);
    }

    @Test
    void rejectsUploadWhenCurrentUserDoesNotOwnApplication() {
        when(currentUserProvider.getCurrentUserId(jwt)).thenReturn(USER_ID);
        when(applicationRepository.findByIdAndApplicantUserId(APPLICATION_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upload(jwt, APPLICATION_ID, file, request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESTAURANT_APPLICATION_ACCESS_DENIED);
        verifyNoInteractions(fileStorageService);
        verify(documentRepository, never()).saveAndFlush(any());
    }

    private void prepareOwnedApplication(RestaurantPartnerApplication application) {
        when(currentUserProvider.getCurrentUserId(jwt)).thenReturn(USER_ID);
        when(applicationRepository.findByIdAndApplicantUserId(APPLICATION_ID, USER_ID))
                .thenReturn(Optional.of(application));
    }

    private RestaurantPartnerApplication editableApplication() {
        RestaurantPartnerApplication application = new RestaurantPartnerApplication();
        application.setId(APPLICATION_ID);
        application.setApplicantUserId(USER_ID);
        application.setStatus(PartnerApplicationStatus.DRAFT);
        return application;
    }

    private StorageUploadResult uploadResult() {
        return new StorageUploadResult(
                StorageProvider.CLOUDINARY,
                STORAGE_KEY,
                "http://storage.example/license.pdf",
                "https://storage.example/license.pdf",
                "license.pdf",
                "application/pdf",
                file.getSize());
    }
}
