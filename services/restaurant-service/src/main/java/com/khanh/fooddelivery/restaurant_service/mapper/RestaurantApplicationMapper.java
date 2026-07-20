package com.khanh.fooddelivery.restaurant_service.mapper;

import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantApplicationCreateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantApplicationUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantApplicationResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantApplicationSummaryResponse;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantPartnerApplication;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RestaurantApplicationMapper {
    RestaurantPartnerApplication toEntity(RestaurantApplicationCreateRequest r);

    RestaurantApplicationResponse toResponse(RestaurantPartnerApplication e);

    RestaurantApplicationSummaryResponse toSummary(RestaurantPartnerApplication e);

    List<RestaurantApplicationSummaryResponse> toSummaries(List<RestaurantPartnerApplication> e);

    void update(
            RestaurantApplicationUpdateRequest r, @MappingTarget RestaurantPartnerApplication e);
}
