package com.khanh.fooddelivery.cart_service.dto.response.internal;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record InternalCartItemSnapshotResponse(
        UUID cartItemId,
        UUID catalogItemId,
        UUID branchItemId,
        int quantity,
        String note,
        List<InternalSelectedOptionSnapshotResponse> selectedOptions,
        String itemName,
        String imageUrl,
        BigDecimal baseUnitPrice,
        BigDecimal optionUnitPrice,
        BigDecimal unitPrice,
        BigDecimal originalPrice) {}
