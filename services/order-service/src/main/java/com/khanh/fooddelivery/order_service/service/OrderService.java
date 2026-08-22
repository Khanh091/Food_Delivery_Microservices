package com.khanh.fooddelivery.order_service.service;

import com.khanh.fooddelivery.order_service.dto.request.CreateOrderRequest;
import com.khanh.fooddelivery.order_service.dto.request.RejectOrderRequest;
import com.khanh.fooddelivery.order_service.dto.response.OrderResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public interface OrderService {
    OrderResponse create(Jwt jwt, CreateOrderRequest request);
    List<OrderResponse> mine(Jwt jwt);
    List<OrderResponse> restaurant(Jwt jwt, UUID restaurantId);
    OrderResponse accept(Jwt jwt, UUID orderId);
    OrderResponse reject(Jwt jwt, UUID orderId, RejectOrderRequest request);
    void deliveryAssigned(UUID orderId); void pickedUp(UUID orderId); void delivered(UUID orderId); void matchingFailed(UUID orderId);
    void paymentPaid(UUID orderId);
    void paymentFailed(UUID orderId);
    void paymentCollected(UUID orderId);
}
