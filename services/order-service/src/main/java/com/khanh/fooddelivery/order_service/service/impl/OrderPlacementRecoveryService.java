package com.khanh.fooddelivery.order_service.service.impl;

import com.khanh.fooddelivery.order_service.dto.response.OrderResponse;
import com.khanh.fooddelivery.order_service.mapper.OrderMapper;
import com.khanh.fooddelivery.order_service.repository.OrderRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderPlacementRecoveryService {

    private final OrderRepository orders;
    private final OrderMapper orderMapper;

    @Transactional(readOnly = true)
    public OrderResponse findResponse(UUID orderId) {
        return orders.findWithItemsById(orderId)
                .map(orderMapper::toResponse)
                .orElse(null);
    }
}
