package com.khanh.fooddelivery.restaurant_service.dto.request;

import com.khanh.fooddelivery.restaurant_service.enums.ApplicationDocumentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ApplicationDocumentUploadRequest(
        @NotNull ApplicationDocumentType documentType,
        @Size(max = 100) String documentNumber,
        LocalDate issuedAt,
        LocalDate expiresAt) {}
