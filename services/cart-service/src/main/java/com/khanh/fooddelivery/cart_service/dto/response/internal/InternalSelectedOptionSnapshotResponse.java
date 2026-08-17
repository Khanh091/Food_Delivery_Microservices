package com.khanh.fooddelivery.cart_service.dto.response.internal;

import java.math.BigDecimal;
import java.util.UUID;

public record InternalSelectedOptionSnapshotResponse(
        UUID optionGroupId,
        UUID optionValueId,
        String groupName,
        String valueName,
        BigDecimal additionalPrice) {}
