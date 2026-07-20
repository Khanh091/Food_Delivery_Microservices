package com.khanh.fooddelivery.restaurant_service.dto.request;
import com.khanh.fooddelivery.restaurant_service.enums.ApplicationDocumentType; import jakarta.validation.constraints.*; import java.time.LocalDate;
public record ApplicationDocumentCreateRequest(@NotNull ApplicationDocumentType documentType,@Size(max=100) String documentNumber,@Size(max=500) String storageKey,@NotBlank @Size(max=1000) String fileUrl,@Size(max=255) String fileName,@Size(max=100) String mimeType,@PositiveOrZero Long fileSize,LocalDate issuedAt,LocalDate expiresAt){}
