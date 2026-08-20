package com.khanh.fooddelivery.tracking_service.client;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "driver-service")
public interface DriverServiceClient {
    @GetMapping("/internal/v1/drivers/{driverId}/active")
    boolean active(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization, @PathVariable UUID driverId);
}
