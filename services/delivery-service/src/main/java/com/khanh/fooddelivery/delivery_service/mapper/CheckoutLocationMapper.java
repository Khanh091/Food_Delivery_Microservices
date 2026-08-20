package com.khanh.fooddelivery.delivery_service.mapper;

import com.khanh.fooddelivery.delivery_service.dto.response.CheckoutTemporaryLocationResponse;
import com.khanh.fooddelivery.delivery_service.model.CheckoutTemporaryLocation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CheckoutLocationMapper {

    CheckoutTemporaryLocationResponse toResponse(CheckoutTemporaryLocation location);
}
