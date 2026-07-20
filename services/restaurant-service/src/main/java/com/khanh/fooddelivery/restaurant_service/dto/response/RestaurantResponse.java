package com.khanh.fooddelivery.restaurant_service.dto.response;

import com.khanh.fooddelivery.restaurant_service.enums.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RestaurantResponse(
        UUID id,
        UUID ownerUserId,
        UUID partnerApplicationId,
        String restaurantCode,
        String name,
        String legalName,
        String description,
        String logoUrl,
        String coverImageUrl,
        String phoneNumber,
        String email,
        String taxCode,
        RestaurantStatus status,
        RestaurantVerificationStatus verificationStatus,
        BigDecimal averageRating,
        long totalReviews,
        Instant createdAt,
        Instant updatedAt,
        long version) {}
