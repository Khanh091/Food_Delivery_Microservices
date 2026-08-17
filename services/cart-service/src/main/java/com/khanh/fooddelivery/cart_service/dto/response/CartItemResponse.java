package com.khanh.fooddelivery.cart_service.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartItemResponse(
        UUID cartItemId,
        UUID catalogItemId,
        UUID branchItemId,
        String name,
        String imageUrl,
        int quantity,
        String note,
        List<SelectedOptionResponse> selectedOptions,
        BigDecimal baseUnitPrice,
        BigDecimal optionUnitPrice,
        BigDecimal unitPrice,
        BigDecimal originalPrice,
        BigDecimal lineTotal) {}
