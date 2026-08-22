package com.khanh.fooddelivery.order_service.dto.response;

import com.khanh.fooddelivery.order_service.enums.OrderStatus;
import com.khanh.fooddelivery.order_service.enums.PaymentMethod;
import com.khanh.fooddelivery.order_service.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(UUID id, String orderCode, UUID restaurantId, String restaurantName, UUID branchId,
                            String branchName, OrderStatus status, String currency, BigDecimal itemsSubtotal,
                            BigDecimal deliveryFee, BigDecimal discountAmount, BigDecimal totalAmount,
                            String addressDisplayLabel, String recipientName, String recipientPhone, String addressLine,
                            String formattedAddress, String ward, String district, String city, BigDecimal latitude,
                            BigDecimal longitude, String rejectionReason, Instant createdAt, List<Item> items,
                            PaymentMethod paymentMethod, PaymentStatus paymentStatus, UUID paymentId,
                            UUID feePolicyId, Integer feePolicyVersion, BigDecimal restaurantCommissionAmount,
                            BigDecimal restaurantNetAmount, BigDecimal driverCommissionAmount,
                            BigDecimal driverNetAmount, BigDecimal platformRevenueAmount) {
    public OrderResponse(
            UUID id,
            String orderCode,
            UUID restaurantId,
            String restaurantName,
            UUID branchId,
            String branchName,
            OrderStatus status,
            String currency,
            BigDecimal itemsSubtotal,
            BigDecimal deliveryFee,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            String addressDisplayLabel,
            String recipientName,
            String recipientPhone,
            String addressLine,
            String rejectionReason,
            Instant createdAt,
            List<Item> items
    ) {
        this(id, orderCode, restaurantId, restaurantName, branchId, branchName, status, currency, itemsSubtotal,
                deliveryFee, discountAmount, totalAmount, addressDisplayLabel, recipientName, recipientPhone,
                addressLine, null, null, null, null, null, null, rejectionReason, createdAt, items,
                null, null, null, null, null, null, null, null, null, null);
    }

    public record Item(UUID id, UUID catalogItemId, String name, String imageUrl, BigDecimal unitPrice, int quantity,
                       BigDecimal lineTotal, String note, List<Option> options) {}
    public record Option(UUID optionGroupId, UUID optionValueId, String groupName, String valueName,
                         BigDecimal additionalPrice) {}
}
