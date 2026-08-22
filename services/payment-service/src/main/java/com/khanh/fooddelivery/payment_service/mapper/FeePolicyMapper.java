package com.khanh.fooddelivery.payment_service.mapper;

import com.khanh.fooddelivery.payment_service.dto.response.FeePolicyResponse;
import com.khanh.fooddelivery.payment_service.entity.FeePolicy;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FeePolicyMapper {
    FeePolicyResponse toResponse(FeePolicy policy);
}
