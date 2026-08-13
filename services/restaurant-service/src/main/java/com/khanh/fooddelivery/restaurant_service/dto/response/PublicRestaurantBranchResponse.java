package com.khanh.fooddelivery.restaurant_service.dto.response;

import java.util.List;
import java.util.UUID;

public record PublicRestaurantBranchResponse(
        UUID restaurantId,
        String restaurantName,
        String restaurantDescription,
        String restaurantLogoUrl,
        String restaurantCoverImageUrl,
        UUID branchId,
        String branchName,
        String phoneNumber,
        String addressLine,
        String ward,
        String district,
        String city,
        boolean acceptingOrders,
        List<PublicBranchBusinessHourResponse> businessHours) {}
