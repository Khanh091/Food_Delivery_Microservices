package com.khanh.fooddelivery.driver_service.client;

import com.khanh.fooddelivery.driver_service.client.dto.response.ApiResponse;
import com.khanh.fooddelivery.driver_service.client.dto.response.CurrentUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service", contextId = "driverUserServiceClient")
public interface UserServiceClient {

    @GetMapping("/api/v1/users/me")
    ApiResponse<CurrentUserResponse> getCurrentUser(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    );
}
