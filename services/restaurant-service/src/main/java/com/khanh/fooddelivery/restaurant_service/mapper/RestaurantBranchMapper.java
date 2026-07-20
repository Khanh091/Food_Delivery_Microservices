package com.khanh.fooddelivery.restaurant_service.mapper;

import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantBranchCreateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantBranchUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantBranchResponse;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantBranch;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RestaurantBranchMapper {
    RestaurantBranch toEntity(RestaurantBranchCreateRequest r);

    @Mapping(target = "restaurantId", source = "restaurant.id")
    RestaurantBranchResponse toResponse(RestaurantBranch e);

    List<RestaurantBranchResponse> toResponses(List<RestaurantBranch> e);

    void update(RestaurantBranchUpdateRequest r, @MappingTarget RestaurantBranch e);
}
