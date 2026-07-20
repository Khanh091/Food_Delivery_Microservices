package com.khanh.fooddelivery.restaurant_service.service.impl;

import com.khanh.fooddelivery.restaurant_service.dto.request.ApplicationDocumentUploadRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.ApplicationDocumentVerificationRequest;
import com.khanh.fooddelivery.restaurant_service.dto.response.ApplicationDocumentResponse;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantApplicationDocument;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantPartnerApplication;
import com.khanh.fooddelivery.restaurant_service.enums.DocumentVerificationStatus;
import com.khanh.fooddelivery.restaurant_service.enums.PartnerApplicationStatus;
import com.khanh.fooddelivery.restaurant_service.exception.AppException;
import com.khanh.fooddelivery.restaurant_service.exception.ErrorCode;
import com.khanh.fooddelivery.restaurant_service.mapper.ApplicationDocumentMapper;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantApplicationDocumentRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantPartnerApplicationRepository;
import com.khanh.fooddelivery.restaurant_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.restaurant_service.service.ApplicationDocumentService;
import com.khanh.fooddelivery.restaurant_service.storage.FileStorageProperties;
import com.khanh.fooddelivery.restaurant_service.storage.FileStorageService;
import com.khanh.fooddelivery.restaurant_service.storage.FileUploadValidator;
import com.khanh.fooddelivery.restaurant_service.storage.StorageProvider;
import com.khanh.fooddelivery.restaurant_service.storage.StorageUploadResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationDocumentServiceImpl implements ApplicationDocumentService {

    private final RestaurantPartnerApplicationRepository applicationRepository;
    private final RestaurantApplicationDocumentRepository documentRepository;
    private final ApplicationDocumentMapper documentMapper;
    private final CurrentUserProvider currentUserProvider;
    private final FileStorageService fileStorageService;
    private final FileStorageProperties storageProperties;
    private final FileUploadValidator fileUploadValidator;

    @Override
    public ApplicationDocumentResponse upload(
            Jwt jwt,
            UUID applicationId,
            MultipartFile file,
            ApplicationDocumentUploadRequest request) {
        RestaurantPartnerApplication application = findOwnedApplication(jwt, applicationId);
        requireEditable(application);
        validateDates(request.issuedAt(), request.expiresAt());
        String sanitizedFilename = fileUploadValidator.validateAndSanitize(file);

        UUID documentId = UUID.randomUUID();
        String resourceName = documentId + "-" + UUID.randomUUID();
        String folder = buildApplicationFolder(applicationId);
        StorageUploadResult uploadResult = fileStorageService.upload(file, folder, resourceName);

        RestaurantApplicationDocument document = new RestaurantApplicationDocument();
        document.setId(documentId);
        document.setApplication(application);
        document.setDocumentType(request.documentType());
        document.setDocumentNumber(request.documentNumber());
        document.setStorageProvider(uploadResult.provider());
        document.setStorageKey(uploadResult.storageKey());
        document.setFileUrl(preferredUrl(uploadResult));
        document.setFileName(sanitizedFilename);
        document.setMimeType(uploadResult.contentType());
        document.setFileSize(uploadResult.fileSize());
        document.setVerificationStatus(DocumentVerificationStatus.PENDING);
        document.setIssuedAt(request.issuedAt());
        document.setExpiresAt(request.expiresAt());

        try {
            return documentMapper.toResponse(documentRepository.saveAndFlush(document));
        } catch (RuntimeException databaseException) {
            compensateUpload(documentId, uploadResult);
            throw databaseException;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationDocumentResponse> findAll(
            Jwt jwt, UUID applicationId, boolean administrator) {
        RestaurantPartnerApplication application = findRequiredApplication(applicationId);
        if (!administrator
                && !application
                        .getApplicantUserId()
                        .equals(currentUserProvider.getCurrentUserId(jwt))) {
            throw new AppException(ErrorCode.RESTAURANT_APPLICATION_ACCESS_DENIED);
        }
        return documentMapper.toResponses(
                documentRepository.findAllByApplicationIdOrderByCreatedAtAsc(applicationId));
    }

    @Override
    @Transactional
    public void delete(Jwt jwt, UUID applicationId, UUID documentId) {
        RestaurantPartnerApplication application = findOwnedApplication(jwt, applicationId);
        requireEditable(application);
        RestaurantApplicationDocument document = findRequiredDocument(applicationId, documentId);
        requireActiveProvider(document);

        fileStorageService.delete(document.getStorageKey());
        documentRepository.delete(document);
        documentRepository.flush();
    }

    @Override
    @Transactional
    public ApplicationDocumentResponse verify(Jwt jwt, UUID applicationId, UUID documentId) {
        RestaurantApplicationDocument document = findRequiredDocument(applicationId, documentId);
        document.setVerificationStatus(DocumentVerificationStatus.VERIFIED);
        document.setVerifiedAt(Instant.now());
        document.setVerifiedByUserId(currentUserProvider.getCurrentUserId(jwt));
        document.setRejectionReason(null);
        return documentMapper.toResponse(document);
    }

    @Override
    @Transactional
    public ApplicationDocumentResponse reject(
            Jwt jwt,
            UUID applicationId,
            UUID documentId,
            ApplicationDocumentVerificationRequest request) {
        if (request.reason() == null || request.reason().isBlank()) {
            throw new AppException(
                    ErrorCode.COMMON_VALIDATION_ERROR, "Rejection reason is required");
        }
        RestaurantApplicationDocument document = findRequiredDocument(applicationId, documentId);
        document.setVerificationStatus(DocumentVerificationStatus.REJECTED);
        document.setVerifiedAt(Instant.now());
        document.setVerifiedByUserId(currentUserProvider.getCurrentUserId(jwt));
        document.setRejectionReason(request.reason());
        return documentMapper.toResponse(document);
    }

    private RestaurantPartnerApplication findOwnedApplication(Jwt jwt, UUID applicationId) {
        return applicationRepository
                .findByIdAndApplicantUserId(
                        applicationId, currentUserProvider.getCurrentUserId(jwt))
                .orElseThrow(
                        () -> new AppException(ErrorCode.RESTAURANT_APPLICATION_ACCESS_DENIED));
    }

    private RestaurantPartnerApplication findRequiredApplication(UUID applicationId) {
        return applicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new AppException(ErrorCode.RESTAURANT_APPLICATION_NOT_FOUND));
    }

    private RestaurantApplicationDocument findRequiredDocument(
            UUID applicationId, UUID documentId) {
        return documentRepository
                .findByIdAndApplicationId(documentId, applicationId)
                .orElseThrow(() -> new AppException(ErrorCode.APPLICATION_DOCUMENT_NOT_FOUND));
    }

    private void requireEditable(RestaurantPartnerApplication application) {
        if (Arrays.stream(
                        new PartnerApplicationStatus[] {
                            PartnerApplicationStatus.DRAFT,
                            PartnerApplicationStatus.NEEDS_MORE_INFORMATION
                        })
                .noneMatch(status -> status == application.getStatus())) {
            throw new AppException(
                    ErrorCode.INVALID_APPLICATION_STATUS_TRANSITION,
                    "Documents cannot be changed while application is " + application.getStatus());
        }
    }

    private void validateDates(LocalDate issuedAt, LocalDate expiresAt) {
        if (issuedAt != null && expiresAt != null && expiresAt.isBefore(issuedAt)) {
            throw new AppException(
                    ErrorCode.COMMON_VALIDATION_ERROR, "expiresAt must not be before issuedAt");
        }
    }

    private String buildApplicationFolder(UUID applicationId) {
        String baseFolder =
                fileStorageService.getProvider() == StorageProvider.CLOUDINARY
                        ? storageProperties.getCloudinary().getBaseFolder()
                        : storageProperties.getS3().getBaseFolder();
        String normalizedBaseFolder =
                baseFolder == null
                        ? ""
                        : baseFolder.replace('\\', '/').replaceAll("^/+", "").replaceAll("/+$", "");
        return normalizedBaseFolder + "/applications/" + applicationId;
    }

    private String preferredUrl(StorageUploadResult uploadResult) {
        return uploadResult.secureUrl() == null || uploadResult.secureUrl().isBlank()
                ? uploadResult.url()
                : uploadResult.secureUrl();
    }

    private void compensateUpload(UUID documentId, StorageUploadResult uploadResult) {
        try {
            fileStorageService.delete(uploadResult.storageKey());
        } catch (RuntimeException cleanupException) {
            log.error(
                    "Storage cleanup failed for document {} using provider {}",
                    documentId,
                    uploadResult.provider(),
                    cleanupException);
        }
    }

    private void requireActiveProvider(RestaurantApplicationDocument document) {
        if (document.getStorageProvider() != fileStorageService.getProvider()) {
            throw new AppException(
                    ErrorCode.FILE_STORAGE_NOT_CONFIGURED,
                    "The document storage provider is not active");
        }
    }
}
