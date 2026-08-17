package com.khanh.fooddelivery.cart_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "catalog-service")
public interface CatalogServiceClient {
    @PostMapping("/internal/v1/catalog/cart-items/validate")
    ApiResponse<CartItemValidationResponse> validateCartItem(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestBody CartItemValidationRequest request);

    record CartItemValidationRequest(
            UUID restaurantId, UUID branchId, UUID catalogItemId, List<UUID> selectedOptionValueIds) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ApiResponse<T>(boolean success, String code, String message, T data, Instant timestamp) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CartItemValidationResponse(
            UUID catalogItemId,
            UUID branchItemId,
            String itemName,
            String primaryImageUrl,
            BigDecimal sellingPrice,
            BigDecimal originalPrice,
            String currency,
            List<SelectedOptionResponse> selectedOptions,
            BigDecimal optionUnitPrice,
            BigDecimal finalUnitPrice) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SelectedOptionResponse(
            UUID optionGroupId,
            UUID optionValueId,
            String groupName,
            String valueName,
            BigDecimal additionalPrice) {}
}
