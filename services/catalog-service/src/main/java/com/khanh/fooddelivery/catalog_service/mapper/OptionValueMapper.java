package com.khanh.fooddelivery.catalog_service.mapper;

import com.khanh.fooddelivery.catalog_service.dto.request.OptionValueCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.OptionValueUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.OptionValueResponse;
import com.khanh.fooddelivery.catalog_service.entity.OptionValue;
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
public interface OptionValueMapper {
    OptionValue toEntity(OptionValueCreateRequest request);

    @Mapping(target = "optionGroupId", source = "optionGroup.id")
    OptionValueResponse toResponse(OptionValue entity);

    List<OptionValueResponse> toResponses(List<OptionValue> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "optionGroup", ignore = true)
    @Mapping(target = "isAvailable", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    void update(OptionValueUpdateRequest request, @MappingTarget OptionValue entity);
}
