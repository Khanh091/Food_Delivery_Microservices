package com.khanh.fooddelivery.order_service.client;

import com.khanh.fooddelivery.order_service.client.dto.response.CurrentUserResponse;
import com.khanh.fooddelivery.order_service.client.dto.response.InternalUserAddressResponse;
import com.khanh.fooddelivery.order_service.common.response.ApiResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/v1/users/me")
    ApiResponse<CurrentUserResponse> getCurrentUser(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    );

    @GetMapping("/internal/v1/users/me/addresses/{addressId}")
    ApiResponse<InternalUserAddressResponse> getOwnedAddress(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable UUID addressId
    );

}
