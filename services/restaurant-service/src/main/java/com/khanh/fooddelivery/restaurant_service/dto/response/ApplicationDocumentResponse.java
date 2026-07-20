package com.khanh.fooddelivery.restaurant_service.dto.response;

import com.khanh.fooddelivery.restaurant_service.enums.ApplicationDocumentType;
import com.khanh.fooddelivery.restaurant_service.enums.DocumentVerificationStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ApplicationDocumentResponse(
        UUID id,
        UUID applicationId,
        ApplicationDocumentType documentType,
        String documentNumber,
        String fileName,
        String fileUrl,
        String mimeType,
        Long fileSize,
        DocumentVerificationStatus verificationStatus,
        LocalDate issuedAt,
        LocalDate expiresAt,
        Instant createdAt,
        Instant updatedAt) {}
