package com.khanh.fooddelivery.payment_service.client;

import com.khanh.fooddelivery.payment_service.client.dto.response.CurrentUserResponse;
import com.khanh.fooddelivery.payment_service.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service")
public interface UserServiceClient {
    @GetMapping("/api/v1/users/me")
    ApiResponse<CurrentUserResponse> getCurrentUser(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization);
}
