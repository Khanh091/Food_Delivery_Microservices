package com.khanh.fooddelivery.catalog_service.dto.request;

import com.khanh.fooddelivery.catalog_service.enums.CatalogItemType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CatalogItemCreateRequest(
        @NotNull
        UUID restaurantId,
        @NotBlank @Size(max = 255)
        String name,
        @Size(max = 5000)
        String description,
        @NotNull
        CatalogItemType itemType,
        @NotNull @DecimalMin("0.00")
        BigDecimal basePrice,
        @Size(min = 3, max = 3)
        String currency,
        @Min(0)
        Integer preparationTimeMinutes,
        Boolean isVegetarian) {}
