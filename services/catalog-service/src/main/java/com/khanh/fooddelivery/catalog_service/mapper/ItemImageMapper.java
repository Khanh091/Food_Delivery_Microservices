package com.khanh.fooddelivery.catalog_service.mapper;

import com.khanh.fooddelivery.catalog_service.dto.response.ItemImageResponse;
import com.khanh.fooddelivery.catalog_service.entity.ItemImage;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ItemImageMapper {
    @Mapping(target = "itemId", source = "item.id")
    ItemImageResponse toResponse(ItemImage entity);

    List<ItemImageResponse> toResponses(List<ItemImage> entities);
}
