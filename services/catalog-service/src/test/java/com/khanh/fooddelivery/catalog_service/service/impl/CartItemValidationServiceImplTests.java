package com.khanh.fooddelivery.catalog_service.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.khanh.fooddelivery.catalog_service.dto.request.internal.CartItemValidationRequest;
import com.khanh.fooddelivery.catalog_service.exception.AppException;
import com.khanh.fooddelivery.catalog_service.exception.ErrorCode;
import com.khanh.fooddelivery.catalog_service.repository.BranchItemRepository;
import com.khanh.fooddelivery.catalog_service.repository.CatalogItemRepository;
import com.khanh.fooddelivery.catalog_service.repository.ItemImageRepository;
import com.khanh.fooddelivery.catalog_service.repository.OptionGroupRepository;
import com.khanh.fooddelivery.catalog_service.repository.OptionValueRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CartItemValidationServiceImplTests {
    @Test
    void duplicateSelectedOptionsAreRejectedBeforeAnyLookup() {
        CartItemValidationServiceImpl service =
                new CartItemValidationServiceImpl(
                        Mockito.mock(CatalogItemRepository.class),
                        Mockito.mock(BranchItemRepository.class),
                        Mockito.mock(ItemImageRepository.class),
                        Mockito.mock(OptionGroupRepository.class),
                        Mockito.mock(OptionValueRepository.class));
        UUID duplicate = UUID.randomUUID();

        assertThatThrownBy(
                        () ->
                                service.validate(
                                        new CartItemValidationRequest(
                                                UUID.randomUUID(),
                                                UUID.randomUUID(),
                                                UUID.randomUUID(),
                                                List.of(duplicate, duplicate))))
                .isInstanceOf(AppException.class)
                .extracting(exception -> ((AppException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_OPTION_SELECTION);
    }
}
