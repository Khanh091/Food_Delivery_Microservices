package com.khanh.fooddelivery.order_service.dto.response;

import com.khanh.fooddelivery.order_service.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(UUID id, String orderCode, UUID restaurantId, String restaurantName, UUID branchId,
                            String branchName, OrderStatus status, String currency, BigDecimal itemsSubtotal,
                            BigDecimal deliveryFee, BigDecimal discountAmount, BigDecimal totalAmount,
                            String addressDisplayLabel, String recipientName, String recipientPhone, String addressLine,
                            String rejectionReason, Instant createdAt, List<Item> items) {
    public record Item(UUID id, UUID catalogItemId, String name, String imageUrl, BigDecimal unitPrice, int quantity,
                       BigDecimal lineTotal, String note, List<Option> options) {}
    public record Option(UUID optionGroupId, UUID optionValueId, String groupName, String valueName,
                         BigDecimal additionalPrice) {}
}
