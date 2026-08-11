package com.khanh.fooddelivery.catalog_service.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.catalog_service.dto.request.BranchItemCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.BranchItemPriceUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.BranchItemQuantityUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.BranchItemSoldOutRequest;
import com.khanh.fooddelivery.catalog_service.entity.BranchItem;
import com.khanh.fooddelivery.catalog_service.entity.CatalogItem;
import com.khanh.fooddelivery.catalog_service.exception.AppException;
import com.khanh.fooddelivery.catalog_service.exception.ErrorCode;
import com.khanh.fooddelivery.catalog_service.mapper.BranchItemMapper;
import com.khanh.fooddelivery.catalog_service.mapper.ItemPriceHistoryMapper;
import com.khanh.fooddelivery.catalog_service.repository.BranchItemRepository;
import com.khanh.fooddelivery.catalog_service.repository.CatalogItemRepository;
import com.khanh.fooddelivery.catalog_service.repository.ItemPriceHistoryRepository;
import com.khanh.fooddelivery.catalog_service.security.SecurityAuditorAware;
import com.khanh.fooddelivery.catalog_service.service.CatalogAuthorizationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BranchItemServiceImplTests {
    private final UUID restaurantId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();
    private final UUID branchItemId = UUID.randomUUID();

    @Mock private BranchItemRepository branchItemRepository;
    @Mock private CatalogItemRepository itemRepository;
    @Mock private ItemPriceHistoryRepository priceHistoryRepository;
    @Mock private BranchItemMapper branchItemMapper;
    @Mock private ItemPriceHistoryMapper priceHistoryMapper;
    @Mock private CatalogAuthorizationService authorizationService;
    @Mock private SecurityAuditorAware auditorAware;

    private BranchItemServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new BranchItemServiceImpl(
                        branchItemRepository,
                        itemRepository,
                        priceHistoryRepository,
                        branchItemMapper,
                        priceHistoryMapper,
                        authorizationService,
                        auditorAware);
    }

    @Test
    void createAuthorizesBranchAndSetsAvailabilityDefaults() {
        CatalogItem item = item();
        BranchItem branchItem = branchItem();
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(branchItemRepository.existsByBranchIdAndItemId(branchId, itemId)).thenReturn(false);
        when(branchItemMapper.toEntity(any())).thenReturn(branchItem);
        when(branchItemRepository.save(branchItem)).thenReturn(branchItem);

        service.create(createRequest());

        verify(authorizationService).requireBranchCatalogAccess(restaurantId, branchId);
        assertEquals(item, branchItem.getItem());
        assertEquals(Boolean.TRUE, branchItem.getIsAvailable());
        assertEquals(null, branchItem.getSoldOutUntil());
    }

    @Test
    void createDuplicateIsConflict() {
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item()));
        when(branchItemRepository.existsByBranchIdAndItemId(branchId, itemId)).thenReturn(true);

        AppException error =
                assertThrows(AppException.class, () -> service.create(createRequest()));

        assertEquals(ErrorCode.BRANCH_ITEM_ALREADY_EXISTS, error.getErrorCode());
        verify(branchItemRepository, never()).save(any());
    }

    @Test
    void unauthorizedCreateDoesNotSave() {
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item()));
        doThrow(new AppException(ErrorCode.ACCESS_DENIED))
                .when(authorizationService)
                .requireBranchCatalogAccess(restaurantId, branchId);

        assertThrows(AppException.class, () -> service.create(createRequest()));

        verify(branchItemRepository, never()).save(any());
    }

    @Test
    void getUsesPersistedItemRestaurantAndBranch() {
        BranchItem branchItem = branchItem();
        when(branchItemRepository.findById(branchItemId)).thenReturn(Optional.of(branchItem));

        service.get(branchItemId);

        verify(authorizationService).requireBranchCatalogAccess(restaurantId, branchId);
    }

    @Test
    void changingPriceCreatesHistoryInSameServiceOperation() {
        BranchItem branchItem = branchItem();
        UUID userId = UUID.randomUUID();
        when(branchItemRepository.findById(branchItemId)).thenReturn(Optional.of(branchItem));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(userId));
        when(branchItemRepository.save(branchItem)).thenReturn(branchItem);

        service.updatePrice(
                branchItemId,
                new BranchItemPriceUpdateRequest(new BigDecimal("55000"), null, "Market update"));

        ArgumentCaptor<com.khanh.fooddelivery.catalog_service.entity.ItemPriceHistory>
                historyCaptor =
                        ArgumentCaptor.forClass(
                                com.khanh.fooddelivery.catalog_service.entity.ItemPriceHistory
                                        .class);
        verify(priceHistoryRepository).save(historyCaptor.capture());
        assertEquals(new BigDecimal("50000"), historyCaptor.getValue().getOldPrice());
        assertEquals(new BigDecimal("55000"), historyCaptor.getValue().getNewPrice());
        assertEquals(userId, historyCaptor.getValue().getChangedBy());
        assertEquals(new BigDecimal("55000"), branchItem.getSellingPrice());
        verify(branchItemRepository).save(branchItem);
    }

    @Test
    void samePriceDoesNotCreateHistory() {
        BranchItem branchItem = branchItem();
        when(branchItemRepository.findById(branchItemId)).thenReturn(Optional.of(branchItem));
        when(branchItemRepository.save(branchItem)).thenReturn(branchItem);

        service.updatePrice(
                branchItemId,
                new BranchItemPriceUpdateRequest(
                        new BigDecimal("50000"), new BigDecimal("60000"), null));

        verify(priceHistoryRepository, never()).save(any());
        assertEquals(new BigDecimal("60000"), branchItem.getOriginalPrice());
    }

    @Test
    void availableClearsSoldOutUntilAndUnavailableOnlyChangesAvailability() {
        BranchItem branchItem = branchItem();
        branchItem.setSoldOutUntil(Instant.now().plusSeconds(3600));
        when(branchItemRepository.findById(branchItemId)).thenReturn(Optional.of(branchItem));
        when(branchItemRepository.save(branchItem)).thenReturn(branchItem);

        service.markUnavailable(branchItemId);
        assertEquals(Boolean.FALSE, branchItem.getIsAvailable());
        service.markAvailable(branchItemId);

        assertEquals(Boolean.TRUE, branchItem.getIsAvailable());
        assertEquals(null, branchItem.getSoldOutUntil());
    }

    @Test
    void soldOutRequiresFutureTime() {
        AppException error =
                assertThrows(
                        AppException.class,
                        () ->
                                service.markSoldOut(
                                        branchItemId,
                                        new BranchItemSoldOutRequest(
                                                Instant.now().minusSeconds(1))));

        assertEquals(ErrorCode.INVALID_SOLD_OUT_TIME, error.getErrorCode());
    }

    @Test
    void soldOutFutureTimeMakesItemUnavailable() {
        BranchItem branchItem = branchItem();
        Instant soldOutUntil = Instant.now().plusSeconds(3600);
        when(branchItemRepository.findById(branchItemId)).thenReturn(Optional.of(branchItem));
        when(branchItemRepository.save(branchItem)).thenReturn(branchItem);

        service.markSoldOut(branchItemId, new BranchItemSoldOutRequest(soldOutUntil));

        assertEquals(Boolean.FALSE, branchItem.getIsAvailable());
        assertEquals(soldOutUntil, branchItem.getSoldOutUntil());
    }

    @Test
    void updatingQuantityDoesNotImplicitlyChangeAvailability() {
        BranchItem branchItem = branchItem();
        branchItem.setIsAvailable(true);
        when(branchItemRepository.findById(branchItemId)).thenReturn(Optional.of(branchItem));
        when(branchItemRepository.save(branchItem)).thenReturn(branchItem);

        service.updateQuantity(branchItemId, new BranchItemQuantityUpdateRequest(0));

        assertEquals(0, branchItem.getAvailableQuantity());
        assertEquals(Boolean.TRUE, branchItem.getIsAvailable());
    }

    @Test
    void priceHistoryUsesRepositoryNewestFirstMethod() {
        BranchItem branchItem = branchItem();
        when(branchItemRepository.findById(branchItemId)).thenReturn(Optional.of(branchItem));
        when(priceHistoryRepository.findAllByBranchItemIdOrderByCreatedAtDesc(branchItemId))
                .thenReturn(java.util.List.of());
        when(priceHistoryMapper.toResponses(java.util.List.of())).thenReturn(java.util.List.of());

        service.getPriceHistory(branchItemId);

        verify(priceHistoryRepository).findAllByBranchItemIdOrderByCreatedAtDesc(branchItemId);
    }

    private BranchItemCreateRequest createRequest() {
        return new BranchItemCreateRequest(itemId, branchId, new BigDecimal("50000"), null, 10);
    }

    private CatalogItem item() {
        CatalogItem item = new CatalogItem();
        item.setId(itemId);
        item.setRestaurantId(restaurantId);
        return item;
    }

    private BranchItem branchItem() {
        BranchItem branchItem = new BranchItem();
        branchItem.setId(branchItemId);
        branchItem.setBranchId(branchId);
        branchItem.setItem(item());
        branchItem.setSellingPrice(new BigDecimal("50000"));
        branchItem.setIsAvailable(true);
        return branchItem;
    }
}
