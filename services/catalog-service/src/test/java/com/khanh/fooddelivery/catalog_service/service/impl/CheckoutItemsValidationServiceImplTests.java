package com.khanh.fooddelivery.catalog_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.catalog_service.dto.request.internal.CartItemValidationRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.internal.CheckoutItemsValidationRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.internal.CartItemValidationResponse;
import com.khanh.fooddelivery.catalog_service.exception.AppException;
import com.khanh.fooddelivery.catalog_service.exception.ErrorCode;
import com.khanh.fooddelivery.catalog_service.service.CartItemValidationService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CheckoutItemsValidationServiceImplTests {
    @Test
    void batchPreservesCartItemCorrelationAndReusesSingleItemValidator() {
        CartItemValidationService single = Mockito.mock(CartItemValidationService.class);
        CheckoutItemsValidationServiceImpl service = new CheckoutItemsValidationServiceImpl(single);
        UUID restaurantId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID cartItemA = UUID.randomUUID();
        UUID cartItemB = UUID.randomUUID();
        UUID itemA = UUID.randomUUID();
        UUID itemB = UUID.randomUUID();
        when(single.validate(any(CartItemValidationRequest.class)))
                .thenAnswer(invocation -> validated(
                        invocation.<CartItemValidationRequest>getArgument(0).catalogItemId()));

        var response = service.validate(new CheckoutItemsValidationRequest(
                restaurantId, branchId, List.of(
                        new CheckoutItemsValidationRequest.CheckoutItemRequest(cartItemA, itemA, List.of()),
                        new CheckoutItemsValidationRequest.CheckoutItemRequest(cartItemB, itemB, List.of()))));

        assertThat(response.items()).extracting(item -> item.cartItemId()).containsExactly(cartItemA, cartItemB);
        assertThat(response.items()).extracting(item -> item.catalogItemId()).containsExactly(itemA, itemB);
        verify(single, times(2)).validate(any(CartItemValidationRequest.class));
    }

    @Test
    void duplicateCartItemIdsAreRejectedBeforeValidation() {
        CartItemValidationService single = Mockito.mock(CartItemValidationService.class);
        CheckoutItemsValidationServiceImpl service = new CheckoutItemsValidationServiceImpl(single);
        UUID duplicate = UUID.randomUUID();

        assertThatThrownBy(() -> service.validate(new CheckoutItemsValidationRequest(
                UUID.randomUUID(), UUID.randomUUID(), List.of(
                        new CheckoutItemsValidationRequest.CheckoutItemRequest(duplicate, UUID.randomUUID(), List.of()),
                        new CheckoutItemsValidationRequest.CheckoutItemRequest(duplicate, UUID.randomUUID(), List.of())))))
                .isInstanceOf(AppException.class)
                .extracting(exception -> ((AppException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    private CartItemValidationResponse validated(UUID itemId) {
        return new CartItemValidationResponse(itemId, UUID.randomUUID(), "Item", null,
                BigDecimal.TEN, null, "VND", List.of(), BigDecimal.ZERO, BigDecimal.TEN);
    }
}
