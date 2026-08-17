package com.khanh.fooddelivery.cart_service.model;

import java.math.BigDecimal;
import java.util.UUID;

public record SelectedOption(
        UUID optionGroupId,
        UUID optionValueId,
        String groupNameSnapshot,
        String valueNameSnapshot,
        BigDecimal additionalPriceSnapshot) {}
