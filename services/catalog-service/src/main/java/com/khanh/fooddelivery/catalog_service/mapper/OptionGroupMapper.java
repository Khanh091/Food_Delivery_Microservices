package com.khanh.fooddelivery.catalog_service.mapper;

import com.khanh.fooddelivery.catalog_service.dto.request.OptionGroupCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.OptionGroupUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.OptionGroupResponse;
import com.khanh.fooddelivery.catalog_service.entity.OptionGroup;
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
public interface OptionGroupMapper {
    OptionGroup toEntity(OptionGroupCreateRequest request);

    @Mapping(target = "itemId", source = "item.id")
    OptionGroupResponse toResponse(OptionGroup entity);

    List<OptionGroupResponse> toResponses(List<OptionGroup> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "item", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    void update(OptionGroupUpdateRequest request, @MappingTarget OptionGroup entity);
}
