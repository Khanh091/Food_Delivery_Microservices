package com.khanh.fooddelivery.order_service.mapper;

import com.khanh.fooddelivery.order_service.dto.response.OrderResponse;
import com.khanh.fooddelivery.order_service.entity.Order;
import com.khanh.fooddelivery.order_service.entity.OrderItem;
import com.khanh.fooddelivery.order_service.entity.OrderItemOption;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    OrderResponse toResponse(Order order);

    @Mapping(source = "itemName", target = "name")
    OrderResponse.Item toItem(OrderItem item);

    OrderResponse.Option toOption(OrderItemOption option);
}
