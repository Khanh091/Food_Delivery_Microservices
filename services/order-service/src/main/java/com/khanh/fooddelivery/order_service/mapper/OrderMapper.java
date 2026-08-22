package com.khanh.fooddelivery.order_service.mapper;

import com.khanh.fooddelivery.order_service.dto.response.OrderResponse;
import com.khanh.fooddelivery.order_service.entity.Order;
import com.khanh.fooddelivery.order_service.entity.OrderItem;
import com.khanh.fooddelivery.order_service.entity.OrderItemOption;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    @Mapping(source = "formattedAddress", target = "formattedAddress")
    OrderResponse toResponse(Order order);

    @Mapping(source = "itemName", target = "name")
    OrderResponse.Item toItem(OrderItem item);

    @Mapping(source = "optionGroupName", target = "groupName")
    @Mapping(source = "optionValueName", target = "valueName")
    OrderResponse.Option toOption(OrderItemOption option);
}
