package com.khanh.fooddelivery.order_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service")
public interface UserServiceClient {
    @GetMapping("/api/v1/users/me")
    RemoteApiResponse<CurrentUserResponse> getCurrentUser(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization);

    @GetMapping("/internal/v1/users/me/addresses/{addressId}")
    RemoteApiResponse<InternalUserAddressResponse> getOwnedAddress(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization, @PathVariable UUID addressId);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CurrentUserResponse(UUID id) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record InternalUserAddressResponse(
            UUID id, String labelType, String customLabel, String displayLabel, String recipientName,
            String recipientPhone, String addressLine, String ward, String district, String city,
            BigDecimal latitude, BigDecimal longitude, String buildingName, String floor, String entrance,
            String deliveryNote, Long version) {}
}
