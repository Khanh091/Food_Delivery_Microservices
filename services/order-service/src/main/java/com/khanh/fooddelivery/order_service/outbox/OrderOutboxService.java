package com.khanh.fooddelivery.order_service.outbox;

import com.khanh.fooddelivery.order_service.entity.Order;

public interface OrderOutboxService {
    void publishOrderConfirmed(Order order);
}
