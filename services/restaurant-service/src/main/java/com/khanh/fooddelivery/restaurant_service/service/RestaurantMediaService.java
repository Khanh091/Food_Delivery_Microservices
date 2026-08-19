package com.khanh.fooddelivery.restaurant_service.service;

import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantResponse;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;

public interface RestaurantMediaService {

    RestaurantResponse updateLogo(Jwt jwt, UUID restaurantId, MultipartFile file);

    RestaurantResponse updateCover(Jwt jwt, UUID restaurantId, MultipartFile file);
}
