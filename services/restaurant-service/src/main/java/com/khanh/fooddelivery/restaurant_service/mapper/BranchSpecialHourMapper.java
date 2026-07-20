package com.khanh.fooddelivery.restaurant_service.mapper;

import com.khanh.fooddelivery.restaurant_service.dto.request.BranchSpecialHourCreateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.BranchSpecialHourUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.response.BranchSpecialHourResponse;
import com.khanh.fooddelivery.restaurant_service.entity.BranchSpecialHour;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BranchSpecialHourMapper {
    BranchSpecialHour toEntity(BranchSpecialHourCreateRequest r);

    @Mapping(target = "branchId", source = "branch.id")
    BranchSpecialHourResponse toResponse(BranchSpecialHour e);

    List<BranchSpecialHourResponse> toResponses(List<BranchSpecialHour> e);

    void update(BranchSpecialHourUpdateRequest r, @MappingTarget BranchSpecialHour e);
}
