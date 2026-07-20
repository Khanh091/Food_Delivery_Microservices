package com.khanh.fooddelivery.restaurant_service.dto.response;

import com.khanh.fooddelivery.restaurant_service.enums.BankAccountVerificationStatus;
import java.time.Instant;
import java.util.UUID;

public record RestaurantBankAccountResponse(
        UUID id,
        UUID restaurantId,
        String bankCode,
        String bankName,
        String maskedAccountNumber,
        String accountHolderName,
        boolean defaultAccount,
        BankAccountVerificationStatus verificationStatus,
        Instant createdAt,
        Instant updatedAt,
        long version) {}
