package com.khanh.fooddelivery.catalog_service.dto.response;

import com.khanh.fooddelivery.catalog_service.enums.CatalogStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MenuResponse(
        UUID id,
        UUID restaurantId,
        UUID branchId,
        String name,
        String description,
        CatalogStatus status,
        LocalDate availableFrom,
        LocalDate availableUntil,
        Instant createdAt,
        Instant updatedAt) {}
