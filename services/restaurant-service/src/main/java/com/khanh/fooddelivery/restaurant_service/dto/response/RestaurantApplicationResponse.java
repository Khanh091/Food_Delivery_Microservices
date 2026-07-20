package com.khanh.fooddelivery.restaurant_service.dto.response;

import com.khanh.fooddelivery.restaurant_service.enums.BusinessType;
import com.khanh.fooddelivery.restaurant_service.enums.PartnerApplicationStatus;
import java.time.Instant;
import java.util.UUID;

public record RestaurantApplicationResponse(
        UUID id,
        UUID applicantUserId,
        String businessName,
        BusinessType businessType,
        String taxCode,
        String representativeName,
        String representativePhone,
        String representativeEmail,
        String description,
        String city,
        String district,
        String businessAddress,
        Integer expectedBranchCount,
        Integer estimatedDailyOrders,
        String mainCuisine,
        PartnerApplicationStatus status,
        Instant submittedAt,
        Instant reviewedAt,
        UUID reviewedByUserId,
        String rejectionReason,
        Instant createdAt,
        Instant updatedAt,
        long version) {}
