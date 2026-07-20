package com.khanh.fooddelivery.restaurant_service.dto.request;

import com.khanh.fooddelivery.restaurant_service.enums.BusinessType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record RestaurantApplicationCreateRequest(
        @NotBlank @Size(max = 255) String businessName,
        BusinessType businessType,
        @Size(max = 50) String taxCode,
        @NotBlank @Size(max = 150) String representativeName,
        @NotBlank @Size(max = 20) String representativePhone,
        @Email @Size(max = 255) String representativeEmail,
        @Size(max = 5000) String description,
        @NotBlank @Size(max = 150) String city,
        @Size(max = 150) String district,
        @NotBlank @Size(max = 500) String businessAddress,
        @NotNull @Positive Integer expectedBranchCount,
        @PositiveOrZero Integer estimatedDailyOrders,
        @Size(max = 100) String mainCuisine) {}
