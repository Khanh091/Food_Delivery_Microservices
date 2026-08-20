package com.khanh.fooddelivery.delivery_service.mapper;

import com.khanh.fooddelivery.delivery_service.dto.response.DeliveryQuoteResponse;
import com.khanh.fooddelivery.delivery_service.model.DeliveryQuote;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DeliveryQuoteMapper {

    @Mapping(target = "serviceable", constant = "true")
    DeliveryQuoteResponse toResponse(DeliveryQuote quote);
}
