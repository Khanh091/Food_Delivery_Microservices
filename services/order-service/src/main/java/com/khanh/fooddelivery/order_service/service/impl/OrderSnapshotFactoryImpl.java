package com.khanh.fooddelivery.order_service.service.impl;

import com.khanh.fooddelivery.order_service.dto.response.CheckoutPreviewResponse;
import com.khanh.fooddelivery.order_service.entity.Order;
import com.khanh.fooddelivery.order_service.entity.OrderItem;
import com.khanh.fooddelivery.order_service.entity.OrderItemOption;
import com.khanh.fooddelivery.order_service.enums.OrderStatus;
import com.khanh.fooddelivery.order_service.service.OrderSnapshotFactory;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OrderSnapshotFactoryImpl implements OrderSnapshotFactory {
    @Override
    public Order create(UUID customerId, CheckoutPreviewResponse preview) {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setCustomerId(customerId);
        order.setRestaurantId(preview.restaurant().restaurantId());
        order.setBranchId(preview.branch().branchId());
        order.setOrderCode(orderCode(order.getId()));
        order.setRestaurantName(preview.restaurant().restaurantName());
        order.setBranchName(preview.branch().branchName());
        order.setStatus(OrderStatus.PENDING_RESTAURANT);
        order.setCurrency(preview.currency());
        order.setItemsSubtotal(preview.itemsSubtotal());
        order.setDeliveryFee(preview.deliveryFee());
        order.setDiscountAmount(preview.discountAmount());
        order.setTotalAmount(preview.totalAmount());
        var address = preview.address();
        order.setAddressDisplayLabel(address.displayLabel());
        order.setRecipientName(address.recipientName());
        order.setRecipientPhone(address.recipientPhone());
        order.setAddressLine(address.addressLine());
        order.setWard(address.ward());
        order.setDistrict(address.district());
        order.setCity(address.city());
        order.setLatitude(address.latitude());
        order.setLongitude(address.longitude());

        for (int itemIndex = 0; itemIndex < preview.items().size(); itemIndex++) {
            CheckoutPreviewResponse.CheckoutItemResponse source = preview.items().get(itemIndex);
            OrderItem item = new OrderItem();
            item.setId(UUID.randomUUID());
            item.setOrder(order);
            item.setCatalogItemId(source.catalogItemId());
            item.setBranchItemId(source.branchItemId());
            item.setItemName(source.name());
            item.setImageUrl(source.imageUrl());
            item.setUnitPrice(source.unitPrice());
            item.setQuantity(source.quantity());
            item.setLineTotal(source.lineTotal());
            item.setNote(source.note());
            item.setSortOrder(itemIndex);
            for (int optionIndex = 0; optionIndex < source.selectedOptions().size(); optionIndex++) {
                var sourceOption = source.selectedOptions().get(optionIndex);
                OrderItemOption option = new OrderItemOption();
                option.setId(UUID.randomUUID());
                option.setOrderItem(item);
                option.setOptionGroupId(sourceOption.optionGroupId());
                option.setOptionValueId(sourceOption.optionValueId());
                option.setOptionGroupName(sourceOption.groupName());
                option.setOptionValueName(sourceOption.valueName());
                option.setAdditionalPrice(sourceOption.additionalPrice());
                option.setSortOrder(optionIndex);
                item.getOptions().add(option);
            }
            order.getItems().add(item);
        }
        return order;
    }

    private String orderCode(UUID id) {
        return "FD" + id.toString().replace("-", "").substring(0, 10).toUpperCase();
    }
}
