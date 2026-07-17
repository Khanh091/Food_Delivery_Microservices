package com.khanh.fooddelivery.user_service.mapper;

import com.khanh.fooddelivery.user_service.dto.request.UserAddressCreateRequest;
import com.khanh.fooddelivery.user_service.dto.request.UserAddressUpdateRequest;
import com.khanh.fooddelivery.user_service.dto.response.UserAddressResponse;
import com.khanh.fooddelivery.user_service.entity.UserAddress;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserAddressMapper {

    UserAddressResponse toResponse(UserAddress entity);

    List<UserAddressResponse> toResponseList(List<UserAddress> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    UserAddress toEntity(UserAddressCreateRequest request);

    @BeanMapping(
            nullValuePropertyMappingStrategy =
                    NullValuePropertyMappingStrategy.IGNORE
    )
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "isDefault", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateEntity(
            UserAddressUpdateRequest request,
            @MappingTarget UserAddress entity
    );
}
