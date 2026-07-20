package com.khanh.fooddelivery.restaurant_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record RestaurantUpdateRequest(
        @Size(max = 255) String name,
        @Size(max = 255) String legalName,
        @Size(max = 5000) String description,
        @Size(max = 1000) String logoUrl,
        @Size(max = 1000) String coverImageUrl,
        @Size(max = 20) String phoneNumber,
        @Email @Size(max = 255) String email,
        @Size(max = 50) String taxCode) {}
