package com.khanh.fooddelivery.payment_service.mapper;

import com.khanh.fooddelivery.payment_service.dto.response.PayoutResponse;
import com.khanh.fooddelivery.payment_service.entity.Payout;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PayoutMapper {
    PayoutResponse toResponse(Payout payout);
}
