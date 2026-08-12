package com.khanh.fooddelivery.user_service.dto.request;

import com.khanh.fooddelivery.user_service.enums.AddressLabelType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UserAddressCreateRequest(
        @NotNull AddressLabelType labelType,
        @Size(max = 100) String customLabel,
        @NotBlank @Size(max = 255) String recipientName,
        @NotBlank @Size(max = 20) String recipientPhone,
        @NotBlank @Size(max = 500) String addressLine,
        @Size(max = 255) String ward,
        @Size(max = 255) String district,
        @NotBlank @Size(max = 255) String city,
        @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        @Size(max = 255) String buildingName,
        @Size(max = 100) String floor,
        @Size(max = 255) String entrance,
        @Size(max = 500) String deliveryNote,
        Boolean isDefault
) {
}
