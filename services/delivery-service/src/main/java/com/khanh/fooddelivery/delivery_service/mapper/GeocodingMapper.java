package com.khanh.fooddelivery.delivery_service.mapper;

import com.khanh.fooddelivery.delivery_service.dto.response.LocationSearchCandidateResponse;
import com.khanh.fooddelivery.delivery_service.dto.response.ReverseGeocodeResponse;
import com.khanh.fooddelivery.delivery_service.service.GeocodingProvider;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GeocodingMapper {

    @Mapping(target = "name", source = "addressLine")
    LocationSearchCandidateResponse toSearchResponse(GeocodingProvider.GeocodedLocation location);

    ReverseGeocodeResponse toReverseResponse(GeocodingProvider.GeocodedLocation location);

    @Mapping(target = "latitude", source = "requestedLatitude")
    @Mapping(target = "longitude", source = "requestedLongitude")
    ReverseGeocodeResponse toReverseResponse(
            GeocodingProvider.GeocodedLocation location,
            BigDecimal requestedLatitude,
            BigDecimal requestedLongitude
    );
}
