package com.khanh.fooddelivery.catalog_service.mapper;

import com.khanh.fooddelivery.catalog_service.dto.request.MenuCategoryCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.MenuCategoryUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.MenuCategoryResponse;
import com.khanh.fooddelivery.catalog_service.entity.MenuCategory;
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
public interface MenuCategoryMapper {
    MenuCategory toEntity(MenuCategoryCreateRequest request);

    @Mapping(target = "menuId", source = "menu.id")
    MenuCategoryResponse toResponse(MenuCategory entity);

    List<MenuCategoryResponse> toResponses(List<MenuCategory> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "menu", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    void update(MenuCategoryUpdateRequest request, @MappingTarget MenuCategory entity);
}
