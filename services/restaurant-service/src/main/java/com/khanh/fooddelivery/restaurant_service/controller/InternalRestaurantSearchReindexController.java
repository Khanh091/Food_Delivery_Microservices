package com.khanh.fooddelivery.restaurant_service.controller;

import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantSearchReindexResponse;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantSearchReindexService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/restaurants")
@RequiredArgsConstructor
public class InternalRestaurantSearchReindexController {
    private final RestaurantSearchReindexService service;
    @PostMapping("/search-reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public RestaurantSearchReindexResponse searchReindex() { return service.enqueueCurrentRestaurantSnapshot(); }
}
