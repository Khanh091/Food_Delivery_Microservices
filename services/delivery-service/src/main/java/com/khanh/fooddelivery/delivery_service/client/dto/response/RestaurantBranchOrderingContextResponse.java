package com.khanh.fooddelivery.delivery_service.client.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;
@JsonIgnoreProperties(ignoreUnknown = true)
public record RestaurantBranchOrderingContextResponse(
        UUID restaurantId,
        String restaurantName,
        boolean restaurantActive,
        UUID branchId,
        String branchName,
        boolean branchActive,
        boolean acceptingOrders,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
