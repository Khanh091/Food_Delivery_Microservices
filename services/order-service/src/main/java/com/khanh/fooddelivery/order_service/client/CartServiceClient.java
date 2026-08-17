package com.khanh.fooddelivery.order_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "cart-service")
public interface CartServiceClient {
    @GetMapping("/internal/v1/carts/me")
    RemoteApiResponse<InternalCartSnapshotResponse> getCurrentSnapshot(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record InternalCartSnapshotResponse(
            UUID ownerUserId, UUID restaurantId, UUID branchId, String currency,
            List<InternalCartItemSnapshotResponse> items, long version, Instant createdAt, Instant updatedAt) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record InternalCartItemSnapshotResponse(
            UUID cartItemId, UUID catalogItemId, UUID branchItemId, int quantity, String note,
            List<InternalSelectedOptionSnapshotResponse> selectedOptions, String itemName, String imageUrl,
            BigDecimal baseUnitPrice, BigDecimal optionUnitPrice, BigDecimal unitPrice, BigDecimal originalPrice) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record InternalSelectedOptionSnapshotResponse(
            UUID optionGroupId, UUID optionValueId, String groupName, String valueName, BigDecimal additionalPrice) {}
}
