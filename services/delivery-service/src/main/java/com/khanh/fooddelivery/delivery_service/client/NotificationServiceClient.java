package com.khanh.fooddelivery.delivery_service.client;

import com.khanh.fooddelivery.delivery_service.client.dto.request.DriverOfferNotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "notification-service")
public interface NotificationServiceClient {

    @PostMapping("/internal/v1/notifications/driver-offers")
    void notifyDriverOffer(
            @RequestHeader("X-Internal-Api-Key") String internalApiKey,
            @RequestBody DriverOfferNotificationRequest request
    );
}
