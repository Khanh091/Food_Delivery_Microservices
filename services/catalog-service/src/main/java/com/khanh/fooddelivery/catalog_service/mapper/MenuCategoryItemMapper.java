package com.khanh.fooddelivery.catalog_service.mapper;

import com.khanh.fooddelivery.catalog_service.dto.request.MenuCategoryItemSortOrderUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.MenuCategoryItemResponse;
import com.khanh.fooddelivery.catalog_service.entity.MenuCategoryItem;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MenuCategoryItemMapper {
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "itemId", source = "item.id")
    MenuCategoryItemResponse toResponse(MenuCategoryItem entity);

    List<MenuCategoryItemResponse> toResponses(List<MenuCategoryItem> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "item", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    void update(
            MenuCategoryItemSortOrderUpdateRequest request, @MappingTarget MenuCategoryItem entity);
}
