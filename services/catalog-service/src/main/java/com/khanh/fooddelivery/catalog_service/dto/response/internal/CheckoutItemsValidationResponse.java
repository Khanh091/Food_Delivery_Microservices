package com.khanh.fooddelivery.catalog_service.dto.response.internal;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CheckoutItemsValidationResponse(List<ValidatedCheckoutItemResponse> items) {
    public record ValidatedCheckoutItemResponse(
            UUID cartItemId,
            UUID catalogItemId,
            UUID branchItemId,
            String itemName,
            String primaryImageUrl,
            BigDecimal sellingPrice,
            BigDecimal originalPrice,
            String currency,
            List<CartItemValidationResponse.SelectedOptionResponse> selectedOptions,
            BigDecimal optionUnitPrice,
            BigDecimal finalUnitPrice) {}
}
