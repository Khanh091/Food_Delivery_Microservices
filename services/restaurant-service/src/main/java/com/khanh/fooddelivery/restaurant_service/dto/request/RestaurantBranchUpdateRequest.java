package com.khanh.fooddelivery.restaurant_service.dto.request;

import com.khanh.fooddelivery.restaurant_service.enums.RestaurantBranchStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record RestaurantBranchUpdateRequest(
        @Size(max = 255) String name,
        @Size(max = 20) String phoneNumber,
        @Email @Size(max = 255) String email,
        @Size(max = 500) String addressLine,
        @Size(max = 150) String ward,
        @Size(max = 150) String district,
        @Size(max = 150) String city,
        @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        @DecimalMin("0") BigDecimal minimumOrderAmount,
        @Positive Integer defaultPreparationMinutes,
        RestaurantBranchStatus status) {}
