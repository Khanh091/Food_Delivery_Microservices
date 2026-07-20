package com.khanh.fooddelivery.restaurant_service.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record RestaurantBranchCreateRequest(
        @NotBlank @Size(max = 30) String branchCode,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 20) String phoneNumber,
        @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 500) String addressLine,
        @Size(max = 150) String ward,
        @Size(max = 150) String district,
        @Size(max = 150) String city,
        @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        @NotNull @DecimalMin("0") BigDecimal minimumOrderAmount,
        @NotNull @Positive Integer defaultPreparationMinutes) {}
