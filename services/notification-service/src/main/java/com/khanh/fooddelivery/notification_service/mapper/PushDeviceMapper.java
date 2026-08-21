package com.khanh.fooddelivery.notification_service.mapper;

import com.khanh.fooddelivery.notification_service.dto.response.PushDeviceResponse;
import com.khanh.fooddelivery.notification_service.entity.PushDevice;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PushDeviceMapper {
    PushDeviceResponse toResponse(PushDevice device);
}
