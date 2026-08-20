package com.khanh.fooddelivery.order_service.client.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ValidatedCheckoutItemResponse(
        UUID cartItemId,
        UUID catalogItemId,
        UUID branchItemId,
        String itemName,
        String primaryImageUrl,
        BigDecimal sellingPrice,
        BigDecimal originalPrice,
        String currency,
        List<SelectedOptionResponse> selectedOptions,
        BigDecimal optionUnitPrice,
        BigDecimal finalUnitPrice
) {
}
