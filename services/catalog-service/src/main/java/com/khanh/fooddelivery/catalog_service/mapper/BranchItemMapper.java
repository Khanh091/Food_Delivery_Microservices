package com.khanh.fooddelivery.catalog_service.mapper;

import com.khanh.fooddelivery.catalog_service.dto.request.BranchItemCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.BranchItemResponse;
import com.khanh.fooddelivery.catalog_service.entity.BranchItem;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BranchItemMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "item", ignore = true)
    @Mapping(target = "isAvailable", ignore = true)
    @Mapping(target = "soldOutUntil", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    BranchItem toEntity(BranchItemCreateRequest request);

    @Mapping(target = "itemId", source = "item.id")
    BranchItemResponse toResponse(BranchItem entity);

    List<BranchItemResponse> toResponses(List<BranchItem> entities);
}
