package com.khanh.fooddelivery.order_service.service;

import com.khanh.fooddelivery.order_service.entity.Order;
import com.khanh.fooddelivery.order_service.enums.OrderStatus;
import com.khanh.fooddelivery.order_service.exception.AppException;
import com.khanh.fooddelivery.order_service.exception.ErrorCode;

public final class OrderLifecycle {
    private OrderLifecycle() {}
    public static void paymentPaid(Order order) { move(order, OrderStatus.PENDING_PAYMENT, OrderStatus.PENDING_RESTAURANT); }
    public static void accept(Order order) { move(order, OrderStatus.PENDING_RESTAURANT, OrderStatus.CONFIRMED); }
    public static void reject(Order order) { move(order, OrderStatus.PENDING_RESTAURANT, OrderStatus.REJECTED); }
    public static void preparing(Order order) { move(order, OrderStatus.CONFIRMED, OrderStatus.PREPARING); }
    public static void delivering(Order order) { move(order, OrderStatus.PREPARING, OrderStatus.DELIVERING); }
    public static void completed(Order order) { move(order, OrderStatus.DELIVERING, OrderStatus.COMPLETED); }
    public static void cancelMatching(Order order) { move(order, OrderStatus.CONFIRMED, OrderStatus.CANCELLED); }
    private static void move(Order order, OrderStatus expected, OrderStatus target) {
        if (order.getStatus() != expected) throw new AppException(ErrorCode.ORDER_TRANSITION_INVALID);
        order.setStatus(target);
    }
}
