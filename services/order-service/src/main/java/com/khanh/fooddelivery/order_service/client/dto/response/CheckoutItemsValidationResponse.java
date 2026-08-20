package com.khanh.fooddelivery.order_service.client.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CheckoutItemsValidationResponse(
        List<ValidatedCheckoutItemResponse> items
) {
}
