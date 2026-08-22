package com.khanh.fooddelivery.order_service.client;

import com.khanh.fooddelivery.order_service.client.dto.response.InternalCartItemSnapshotResponse;
import com.khanh.fooddelivery.order_service.client.dto.response.InternalCartSnapshotResponse;
import com.khanh.fooddelivery.order_service.common.response.ApiResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "cart-service")
public interface CartServiceClient {

    @GetMapping("/internal/v1/carts/me/branches/{branchId}")
    ApiResponse<InternalCartSnapshotResponse> getCurrentSnapshot(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable UUID branchId
    );

    @DeleteMapping("/api/v1/carts/branches/{branchId}")
    ApiResponse<Object> clear(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable UUID branchId,
            @RequestParam("expectedCartVersion") long expectedCartVersion
    );

}
