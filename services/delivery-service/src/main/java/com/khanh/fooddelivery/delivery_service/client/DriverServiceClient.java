package com.khanh.fooddelivery.delivery_service.client;

import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "driver-service")
public interface DriverServiceClient {

    @GetMapping("/internal/v1/drivers/available")
    List<UUID> available(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization);

    @PostMapping("/internal/v1/drivers/{driverId}/offers/{deliveryId}/reserve")
    void reserveOffer(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable UUID driverId,
            @PathVariable UUID deliveryId
    );

    @PostMapping("/internal/v1/drivers/{driverId}/offers/{deliveryId}/accept")
    void acceptOffer(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable UUID driverId,
            @PathVariable UUID deliveryId
    );

    @PostMapping("/internal/v1/drivers/{driverId}/offers/{deliveryId}/release")
    void releaseOffer(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable UUID driverId,
            @PathVariable UUID deliveryId
    );

    @PostMapping("/internal/v1/drivers/{driverId}/release/{deliveryId}")
    void release(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable UUID driverId,
            @PathVariable UUID deliveryId
    );
}
