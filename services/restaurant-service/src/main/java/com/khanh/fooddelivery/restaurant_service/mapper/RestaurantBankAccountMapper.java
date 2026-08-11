package com.khanh.fooddelivery.restaurant_service.mapper;

import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantBankAccountResponse;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantBankAccount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RestaurantBankAccountMapper {
    @Mapping(target = "restaurantId", source = "restaurant.id")
    @Mapping(target = "maskedAccountNumber", expression = "java(mask(e.getAccountNumber()))")
    RestaurantBankAccountResponse toResponse(RestaurantBankAccount e);

    default String mask(String n) {
        if (n == null) return null;
        int visible = Math.min(4, n.length());
        return "*".repeat(Math.max(0, n.length() - visible)) + n.substring(n.length() - visible);
    }
}
