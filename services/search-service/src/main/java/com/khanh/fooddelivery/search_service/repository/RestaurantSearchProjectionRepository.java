package com.khanh.fooddelivery.search_service.repository;

import com.khanh.fooddelivery.search_service.document.RestaurantBranchSearchProjection;
import com.khanh.fooddelivery.search_service.document.RestaurantSearchProjection;

public interface RestaurantSearchProjectionRepository {
    void createIndexIfAbsent();
    void recreateIndex();
    void applyRestaurant(RestaurantSearchProjection projection);
    void applyBranch(RestaurantBranchSearchProjection projection, java.util.UUID restaurantId);
}
