package com.khanh.fooddelivery.catalog_service.service.impl;

import com.khanh.fooddelivery.catalog_service.dto.request.internal.CartItemValidationRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.internal.CheckoutItemsValidationRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.internal.CartItemValidationResponse;
import com.khanh.fooddelivery.catalog_service.dto.response.internal.CheckoutItemsValidationResponse;
import com.khanh.fooddelivery.catalog_service.exception.AppException;
import com.khanh.fooddelivery.catalog_service.exception.ErrorCode;
import com.khanh.fooddelivery.catalog_service.service.CartItemValidationService;
import com.khanh.fooddelivery.catalog_service.service.CheckoutItemsValidationService;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckoutItemsValidationServiceImpl implements CheckoutItemsValidationService {
    private final CartItemValidationService cartItemValidationService;

    @Override
    public CheckoutItemsValidationResponse validate(CheckoutItemsValidationRequest request) {
        List<CheckoutItemsValidationRequest.CheckoutItemRequest> items = request.items();
        if (new HashSet<>(items.stream().map(CheckoutItemsValidationRequest.CheckoutItemRequest::cartItemId).toList()).size()
                != items.size()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Duplicate cartItemId");
        }
        return new CheckoutItemsValidationResponse(
                items.stream()
                        .map(item -> toResponse(item, cartItemValidationService.validate(new CartItemValidationRequest(
                                request.restaurantId(), request.branchId(), item.catalogItemId(), item.selectedOptionValueIds()))))
                        .toList());
    }

    private CheckoutItemsValidationResponse.ValidatedCheckoutItemResponse toResponse(
            CheckoutItemsValidationRequest.CheckoutItemRequest item,
            CartItemValidationResponse validated) {
        return new CheckoutItemsValidationResponse.ValidatedCheckoutItemResponse(
                item.cartItemId(),
                validated.catalogItemId(),
                validated.branchItemId(),
                validated.itemName(),
                validated.primaryImageUrl(),
                validated.sellingPrice(),
                validated.originalPrice(),
                validated.currency(),
                validated.selectedOptions(),
                validated.optionUnitPrice(),
                validated.finalUnitPrice());
    }
}
