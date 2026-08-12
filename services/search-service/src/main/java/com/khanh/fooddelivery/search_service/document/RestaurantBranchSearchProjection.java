package com.khanh.fooddelivery.search_service.document;

import java.math.BigDecimal;
import java.util.UUID;
public record RestaurantBranchSearchProjection(UUID branchId, String name, String status, String addressLine,
        String ward, String district, String city, BigDecimal latitude, BigDecimal longitude,
        boolean acceptingOrders, long aggregateVersion, UUID lastEventId) {}
