package com.khanh.fooddelivery.restaurant_service.mapper;

import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantSummaryResponse;
import com.khanh.fooddelivery.restaurant_service.entity.Restaurant;
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
public interface RestaurantMapper {
    @Mapping(target = "partnerApplicationId", source = "partnerApplication.id")
    RestaurantResponse toResponse(Restaurant e);

    RestaurantSummaryResponse toSummary(Restaurant e);

    List<RestaurantSummaryResponse> toSummaries(List<Restaurant> e);

    void update(RestaurantUpdateRequest r, @MappingTarget Restaurant e);
}
