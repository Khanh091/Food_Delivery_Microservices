package com.khanh.fooddelivery.payment_service.mapper;

import com.khanh.fooddelivery.payment_service.dto.response.SettlementResponse;
import com.khanh.fooddelivery.payment_service.entity.Settlement;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SettlementMapper {
    SettlementResponse toResponse(Settlement settlement);
}
