package com.khanh.fooddelivery.cart_service.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartItem(
        UUID id,
        String configurationFingerprint,
        UUID catalogItemId,
        UUID branchItemId,
        int quantity,
        String note,
        List<SelectedOption> selectedOptions,
        String itemNameSnapshot,
        String imageUrlSnapshot,
        BigDecimal baseUnitPriceSnapshot,
        BigDecimal optionUnitPriceSnapshot,
        BigDecimal unitPriceSnapshot,
        BigDecimal originalPriceSnapshot) {
    public BigDecimal lineTotal() {
        return unitPriceSnapshot.multiply(BigDecimal.valueOf(quantity));
    }
}
