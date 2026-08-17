package com.khanh.fooddelivery.order_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "catalog-service")
public interface CatalogServiceClient {
    @PostMapping("/internal/v1/catalog/checkout-items/validate")
    RemoteApiResponse<CheckoutItemsValidationResponse> validateCheckoutItems(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestBody CheckoutItemsValidationRequest request);

    record CheckoutItemsValidationRequest(UUID restaurantId, UUID branchId, List<CheckoutItemRequest> items) {}
    record CheckoutItemRequest(UUID cartItemId, UUID catalogItemId, List<UUID> selectedOptionValueIds) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CheckoutItemsValidationResponse(List<ValidatedCheckoutItemResponse> items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ValidatedCheckoutItemResponse(
            UUID cartItemId, UUID catalogItemId, UUID branchItemId, String itemName, String primaryImageUrl,
            BigDecimal sellingPrice, BigDecimal originalPrice, String currency,
            List<SelectedOptionResponse> selectedOptions, BigDecimal optionUnitPrice, BigDecimal finalUnitPrice) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SelectedOptionResponse(UUID optionGroupId, UUID optionValueId, String groupName, String valueName,
                                  BigDecimal additionalPrice) {}
}
