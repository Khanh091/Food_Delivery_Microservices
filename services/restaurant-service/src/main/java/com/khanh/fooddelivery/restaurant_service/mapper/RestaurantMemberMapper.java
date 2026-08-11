package com.khanh.fooddelivery.restaurant_service.mapper;

import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantMemberResponse;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RestaurantMemberMapper {
    @Mapping(target = "restaurantId", source = "restaurant.id")
    @Mapping(target = "branchId", source = "branch.id")
    RestaurantMemberResponse toResponse(RestaurantMember e);
}
