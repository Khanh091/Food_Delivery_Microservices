package com.khanh.fooddelivery.tracking_service.client;

import com.khanh.fooddelivery.tracking_service.client.dto.response.DriverProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "driver-service", contextId = "trackingDriverServiceClient")
public interface DriverServiceClient {

    @GetMapping("/api/v1/drivers/me/profile")
    DriverProfileResponse profile(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    );
}
