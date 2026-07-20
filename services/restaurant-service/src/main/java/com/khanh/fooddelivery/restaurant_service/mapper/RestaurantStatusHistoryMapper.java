package com.khanh.fooddelivery.restaurant_service.mapper;

import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantStatusHistoryResponse;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantStatusHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RestaurantStatusHistoryMapper {
    @Mapping(target = "restaurantId", source = "restaurant.id")
    RestaurantStatusHistoryResponse toResponse(RestaurantStatusHistory e);
}
