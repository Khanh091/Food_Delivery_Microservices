package com.khanh.fooddelivery.restaurant_service.service;

import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantSearchReindexResponse;

public interface RestaurantSearchReindexService {
    RestaurantSearchReindexResponse enqueueCurrentRestaurantSnapshot();
}
