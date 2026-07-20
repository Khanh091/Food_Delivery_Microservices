package com.khanh.fooddelivery.restaurant_service.mapper;

import com.khanh.fooddelivery.restaurant_service.dto.response.BranchBusinessHourResponse;
import com.khanh.fooddelivery.restaurant_service.entity.BranchBusinessHour;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BranchBusinessHourMapper {
    @Mapping(target = "branchId", source = "branch.id")
    BranchBusinessHourResponse toResponse(BranchBusinessHour e);

    List<BranchBusinessHourResponse> toResponses(List<BranchBusinessHour> e);
}
