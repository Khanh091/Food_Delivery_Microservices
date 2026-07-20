package com.khanh.fooddelivery.restaurant_service.mapper;

import com.khanh.fooddelivery.restaurant_service.dto.response.ApplicationDocumentResponse;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantApplicationDocument;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ApplicationDocumentMapper {
    @Mapping(target = "applicationId", source = "application.id")
    ApplicationDocumentResponse toResponse(RestaurantApplicationDocument e);

    List<ApplicationDocumentResponse> toResponses(List<RestaurantApplicationDocument> e);
}
