package com.khanh.fooddelivery.restaurant_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplicationInformationRequest(@NotBlank @Size(max = 1000) String reason) {}
