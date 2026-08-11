package com.khanh.fooddelivery.catalog_service.mapper;

import com.khanh.fooddelivery.catalog_service.dto.request.CatalogItemCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.CatalogItemUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.CatalogItemResponse;
import com.khanh.fooddelivery.catalog_service.entity.CatalogItem;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CatalogItemMapper {
    CatalogItem toEntity(CatalogItemCreateRequest request);

    CatalogItemResponse toResponse(CatalogItem entity);

    List<CatalogItemResponse> toResponses(List<CatalogItem> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurantId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    void update(CatalogItemUpdateRequest request, @MappingTarget CatalogItem entity);
}
