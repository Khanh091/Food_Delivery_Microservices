package com.khanh.fooddelivery.catalog_service.dto.request;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record MenuUpdateRequest(
        @Size(max = 255) String name,
        @Size(max = 5000) String description,
        LocalDate availableFrom,
        LocalDate availableUntil) {}
