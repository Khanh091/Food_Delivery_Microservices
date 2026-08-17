package com.khanh.fooddelivery.delivery_service.dto.response;

import java.math.BigDecimal;

public record LocationSearchCandidateResponse(
        String providerRefId,
        String formattedAddress,
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        String ward,
        String district,
        String city) {}
