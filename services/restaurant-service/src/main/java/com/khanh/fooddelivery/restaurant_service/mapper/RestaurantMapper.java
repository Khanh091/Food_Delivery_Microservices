package com.khanh.fooddelivery.restaurant_service.mapper;

import com.khanh.fooddelivery.restaurant_service.dto.request.*;
import com.khanh.fooddelivery.restaurant_service.dto.response.*;
import com.khanh.fooddelivery.restaurant_service.entity.*;
import java.util.List;
import org.mapstruct.*;

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
