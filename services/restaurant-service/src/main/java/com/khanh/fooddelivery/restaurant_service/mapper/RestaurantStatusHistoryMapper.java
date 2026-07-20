package com.khanh.fooddelivery.restaurant_service.mapper;

import com.khanh.fooddelivery.restaurant_service.dto.response.*;
import com.khanh.fooddelivery.restaurant_service.entity.*;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RestaurantStatusHistoryMapper {
    @Mapping(target = "restaurantId", source = "restaurant.id")
    RestaurantStatusHistoryResponse toResponse(RestaurantStatusHistory e);
}
