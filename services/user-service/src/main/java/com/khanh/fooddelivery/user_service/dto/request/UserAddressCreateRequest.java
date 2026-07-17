package com.khanh.fooddelivery.user_service.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UserAddressCreateRequest(
        @Size(max = 100) String label,
        @NotBlank @Size(max = 255) String recipientName,
        @NotBlank @Size(max = 20) String recipientPhone,
        @NotBlank @Size(max = 500) String addressLine,
        @Size(max = 255) String ward,
        @Size(max = 255) String district,
        @NotBlank @Size(max = 255) String city,
        @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        @Size(max = 500) String deliveryNote,
        Boolean isDefault
) {
}
