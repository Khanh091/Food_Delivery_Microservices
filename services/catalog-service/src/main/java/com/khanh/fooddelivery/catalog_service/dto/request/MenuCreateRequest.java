package com.khanh.fooddelivery.catalog_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record MenuCreateRequest(
        @NotNull UUID restaurantId,
        @NotNull UUID branchId,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 5000) String description,
        LocalDate availableFrom,
        LocalDate availableUntil) {}
