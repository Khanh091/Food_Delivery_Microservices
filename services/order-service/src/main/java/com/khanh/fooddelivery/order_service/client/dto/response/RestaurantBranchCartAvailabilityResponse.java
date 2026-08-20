package com.khanh.fooddelivery.order_service.client.dto.response;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
@JsonIgnoreProperties(ignoreUnknown = true)
public record RestaurantBranchCartAvailabilityResponse(
        UUID restaurantId,
        String restaurantName,
        boolean restaurantActive,
        UUID branchId,
        String branchName,
        boolean branchActive,
        boolean acceptingOrders
) {
}
