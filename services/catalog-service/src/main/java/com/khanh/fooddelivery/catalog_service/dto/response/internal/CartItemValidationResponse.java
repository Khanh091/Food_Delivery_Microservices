package com.khanh.fooddelivery.catalog_service.dto.response.internal;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartItemValidationResponse(
        UUID catalogItemId,
        UUID branchItemId,
        String itemName,
        String primaryImageUrl,
        BigDecimal sellingPrice,
        BigDecimal originalPrice,
        String currency,
        List<SelectedOptionResponse> selectedOptions,
        BigDecimal optionUnitPrice,
        BigDecimal finalUnitPrice) {
    public record SelectedOptionResponse(
            UUID optionGroupId,
            UUID optionValueId,
            String groupName,
            String valueName,
            BigDecimal additionalPrice) {}
}
