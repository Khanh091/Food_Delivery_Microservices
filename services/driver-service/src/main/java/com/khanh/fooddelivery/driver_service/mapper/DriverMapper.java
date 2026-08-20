package com.khanh.fooddelivery.driver_service.mapper;

import com.khanh.fooddelivery.driver_service.dto.response.DriverAvailabilityResponse;
import com.khanh.fooddelivery.driver_service.dto.response.DriverProfileResponse;
import com.khanh.fooddelivery.driver_service.entity.DriverAvailability;
import com.khanh.fooddelivery.driver_service.entity.DriverProfile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DriverMapper {

    DriverProfileResponse toResponse(DriverProfile profile);

    DriverAvailabilityResponse toResponse(DriverAvailability availability);
}
