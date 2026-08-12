package com.khanh.fooddelivery.search_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemSearchCriteria(
        UUID branchId,
        String query,
        UUID restaurantId,
        String itemType,
        Boolean vegetarian,
        Boolean available,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        int page,
        int size) {}
