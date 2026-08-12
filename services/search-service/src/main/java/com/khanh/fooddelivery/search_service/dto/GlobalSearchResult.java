package com.khanh.fooddelivery.search_service.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record GlobalSearchResult(
        UUID restaurantId,
        UUID branchId,
        String restaurantName,
        String branchName,
        String logoUrl,
        String coverImageUrl,
        String addressLine,
        String ward,
        String district,
        String city,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean acceptingOrders,
        List<MatchingItem> matchingItems) {
    public record MatchingItem(
            UUID itemId, String name, BigDecimal sellingPrice, BigDecimal originalPrice, String currency) {}
}
