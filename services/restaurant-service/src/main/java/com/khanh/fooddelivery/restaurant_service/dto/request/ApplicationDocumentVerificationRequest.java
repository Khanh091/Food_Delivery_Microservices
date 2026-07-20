package com.khanh.fooddelivery.restaurant_service.dto.request;

import jakarta.validation.constraints.Size;

public record ApplicationDocumentVerificationRequest(@Size(max = 500) String reason) {}
