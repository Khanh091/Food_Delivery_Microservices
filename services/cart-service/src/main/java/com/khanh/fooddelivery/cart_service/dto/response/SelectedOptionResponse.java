package com.khanh.fooddelivery.cart_service.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record SelectedOptionResponse(
        UUID optionGroupId,
        UUID optionValueId,
        String groupName,
        String valueName,
        BigDecimal additionalPrice) {}
