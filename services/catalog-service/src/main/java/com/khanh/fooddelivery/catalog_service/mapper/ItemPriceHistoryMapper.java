package com.khanh.fooddelivery.catalog_service.mapper;

import com.khanh.fooddelivery.catalog_service.dto.response.ItemPriceHistoryResponse;
import com.khanh.fooddelivery.catalog_service.entity.ItemPriceHistory;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ItemPriceHistoryMapper {
    ItemPriceHistoryResponse toResponse(ItemPriceHistory entity);

    List<ItemPriceHistoryResponse> toResponses(List<ItemPriceHistory> entities);
}
