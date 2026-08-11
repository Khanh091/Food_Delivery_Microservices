package com.khanh.fooddelivery.catalog_service.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.catalog_service.dto.request.CatalogItemCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.CatalogItemUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.CatalogItemResponse;
import com.khanh.fooddelivery.catalog_service.entity.CatalogItem;
import com.khanh.fooddelivery.catalog_service.enums.CatalogItemType;
import com.khanh.fooddelivery.catalog_service.enums.CatalogStatus;
import com.khanh.fooddelivery.catalog_service.exception.AppException;
import com.khanh.fooddelivery.catalog_service.exception.ErrorCode;
import com.khanh.fooddelivery.catalog_service.mapper.CatalogItemMapper;
import com.khanh.fooddelivery.catalog_service.repository.CatalogItemRepository;
import com.khanh.fooddelivery.catalog_service.service.CatalogAuthorizationService;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogItemServiceImplTests {
    private final UUID restaurantId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();

    @Mock private CatalogItemRepository itemRepository;
    @Mock private CatalogItemMapper itemMapper;
    @Mock private CatalogAuthorizationService authorizationService;

    private CatalogItemServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CatalogItemServiceImpl(itemRepository, itemMapper, authorizationService);
    }

    @Test
    void createAuthorizedItemSetsDefaults() {
        CatalogItem item = item();
        when(itemMapper.toEntity(any())).thenReturn(item);
        when(itemRepository.save(item)).thenReturn(item);
        when(itemMapper.toResponse(item)).thenReturn(response(item));

        service.create(createRequest(null, null));

        verify(authorizationService).requireRestaurantCatalogAccess(restaurantId);
        assertEquals(CatalogStatus.ACTIVE, item.getStatus());
        assertEquals("VND", item.getCurrency());
        assertEquals(Boolean.FALSE, item.getIsVegetarian());
    }

    @Test
    void createUnauthorizedItemDoesNotSave() {
        doThrow(new AppException(ErrorCode.ACCESS_DENIED))
                .when(authorizationService)
                .requireRestaurantCatalogAccess(restaurantId);

        assertThrows(AppException.class, () -> service.create(createRequest("VND", false)));

        verify(itemRepository, never()).save(any());
    }

    @Test
    void getUsesPersistedRestaurantForAuthorization() {
        CatalogItem item = item();
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(itemMapper.toResponse(item)).thenReturn(response(item));

        service.get(itemId);

        verify(authorizationService).requireRestaurantCatalogAccess(restaurantId);
    }

    @Test
    void updatePreservesRestaurantAndStatus() {
        CatalogItem item = item();
        item.setStatus(CatalogStatus.INACTIVE);
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(itemRepository.save(item)).thenReturn(item);
        when(itemMapper.toResponse(item)).thenReturn(response(item));

        service.update(
                itemId,
                new CatalogItemUpdateRequest(
                        "Updated", null, CatalogItemType.DRINK, BigDecimal.TEN, "usd", 5, true));

        verify(itemMapper)
                .update(
                        any(CatalogItemUpdateRequest.class),
                        org.mockito.ArgumentMatchers.same(item));
        assertEquals(restaurantId, item.getRestaurantId());
        assertEquals(CatalogStatus.INACTIVE, item.getStatus());
        assertEquals("USD", item.getCurrency());
    }

    @Test
    void deactivateAuthorizesBeforeUpdatingStatus() {
        CatalogItem item = item();
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(itemRepository.save(item)).thenReturn(item);
        when(itemMapper.toResponse(item)).thenReturn(response(item));

        service.deactivate(itemId);

        assertEquals(CatalogStatus.INACTIVE, item.getStatus());
        verify(authorizationService).requireRestaurantCatalogAccess(restaurantId);
    }

    private CatalogItemCreateRequest createRequest(String currency, Boolean isVegetarian) {
        return new CatalogItemCreateRequest(
                restaurantId,
                "Chicken rice",
                null,
                CatalogItemType.FOOD,
                BigDecimal.TEN,
                currency,
                10,
                isVegetarian);
    }

    private CatalogItem item() {
        CatalogItem item = new CatalogItem();
        item.setId(itemId);
        item.setRestaurantId(restaurantId);
        item.setItemType(CatalogItemType.FOOD);
        item.setBasePrice(BigDecimal.TEN);
        return item;
    }

    private CatalogItemResponse response(CatalogItem item) {
        return new CatalogItemResponse(
                item.getId(),
                item.getRestaurantId(),
                item.getName(),
                item.getDescription(),
                item.getItemType(),
                item.getBasePrice(),
                item.getCurrency(),
                item.getPreparationTimeMinutes(),
                item.getIsVegetarian(),
                item.getStatus(),
                null,
                null);
    }
}
