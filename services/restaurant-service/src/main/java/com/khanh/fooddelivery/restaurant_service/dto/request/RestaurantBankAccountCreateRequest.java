package com.khanh.fooddelivery.restaurant_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestaurantBankAccountCreateRequest(
        @NotBlank @Size(max = 30) String bankCode,
        @Size(max = 150) String bankName,
        @NotBlank @Size(max = 100) String accountNumber,
        @NotBlank @Size(max = 150) String accountHolderName,
        Boolean isDefault) {}
