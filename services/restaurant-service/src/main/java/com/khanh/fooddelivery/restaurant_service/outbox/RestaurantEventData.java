package com.khanh.fooddelivery.restaurant_service.outbox;

import com.khanh.fooddelivery.restaurant_service.entity.Restaurant;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantBranch;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RestaurantEventData {

    private RestaurantEventData() {
    }

    public static Map<String, Object> restaurant(
            Restaurant restaurant,
            String action
    ) {
        Map<String, Object> data = base(action);
        data.put("restaurantId", restaurant.getId());
        data.put("name", restaurant.getName());
        data.put("description", restaurant.getDescription());
        data.put("status", restaurant.getStatus());
        data.put("restaurantCode", restaurant.getRestaurantCode());
        data.put("logoUrl", restaurant.getLogoUrl());
        data.put("coverImageUrl", restaurant.getCoverImageUrl());
        return data;
    }

    public static Map<String, Object> branch(
            RestaurantBranch branch,
            String action
    ) {
        Map<String, Object> data = base(action);
        data.put("branchId", branch.getId());
        data.put("restaurantId", branch.getRestaurant().getId());
        data.put("name", branch.getName());
        data.put("status", branch.getStatus());
        data.put("addressLine", branch.getAddressLine());
        data.put("ward", branch.getWard());
        data.put("district", branch.getDistrict());
        data.put("city", branch.getCity());
        data.put("latitude", branch.getLatitude());
        data.put("longitude", branch.getLongitude());
        data.put("acceptingOrders", branch.isAcceptingOrders());
        return data;
    }

    private static Map<String, Object> base(String action) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("action", action);
        return data;
    }
}