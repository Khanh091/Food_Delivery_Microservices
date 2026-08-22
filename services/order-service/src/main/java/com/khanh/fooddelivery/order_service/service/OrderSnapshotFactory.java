package com.khanh.fooddelivery.order_service.service;

import com.khanh.fooddelivery.order_service.dto.response.CheckoutPreviewResponse;
import com.khanh.fooddelivery.order_service.entity.Order;
import com.khanh.fooddelivery.order_service.enums.PaymentMethod;
import java.util.UUID;

public interface OrderSnapshotFactory {
    Order create(UUID customerId, CheckoutPreviewResponse preview);
    Order create(UUID customerId, CheckoutPreviewResponse preview, PaymentMethod paymentMethod);
}
