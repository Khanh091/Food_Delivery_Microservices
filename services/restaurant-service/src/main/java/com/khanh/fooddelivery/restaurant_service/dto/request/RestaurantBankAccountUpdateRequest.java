package com.khanh.fooddelivery.restaurant_service.dto.request;

import jakarta.validation.constraints.Size;

public record RestaurantBankAccountUpdateRequest(
        @Size(max = 150) String bankName, @Size(max = 150) String accountHolderName) {}
