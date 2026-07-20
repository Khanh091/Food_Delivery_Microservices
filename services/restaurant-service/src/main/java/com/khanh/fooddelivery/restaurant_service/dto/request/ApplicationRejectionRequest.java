package com.khanh.fooddelivery.restaurant_service.dto.request; import jakarta.validation.constraints.*; public record ApplicationRejectionRequest(@NotBlank @Size(max=1000) String reason){}
