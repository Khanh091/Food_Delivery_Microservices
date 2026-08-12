package com.khanh.fooddelivery.catalog_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.catalog_service.entity.BranchItem;
import com.khanh.fooddelivery.catalog_service.entity.CatalogItem;
import com.khanh.fooddelivery.catalog_service.enums.CatalogItemType;
import com.khanh.fooddelivery.catalog_service.enums.CatalogStatus;
import com.khanh.fooddelivery.catalog_service.outbox.CatalogEventType;
import com.khanh.fooddelivery.catalog_service.outbox.OutboxEventService;
import com.khanh.fooddelivery.catalog_service.repository.BranchItemRepository;
import com.khanh.fooddelivery.catalog_service.repository.CatalogItemRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;

class CatalogSearchReindexBatchServiceTests {
    @Test
    void catalogItemBatchWritesCurrentSnapshotToOutbox() {
        CatalogItemRepository catalogItemRepository = Mockito.mock(CatalogItemRepository.class);
        BranchItemRepository branchItemRepository = Mockito.mock(BranchItemRepository.class);
        OutboxEventService outboxEventService = Mockito.mock(OutboxEventService.class);
        CatalogItem item = catalogItem();
        when(catalogItemRepository.findSnapshotIds(any(PageRequest.class)))
                .thenReturn(List.of(item.getId()));
        when(catalogItemRepository.findAllByIdInForUpdate(List.of(item.getId())))
                .thenReturn(List.of(item));
        CatalogSearchReindexBatchService service =
                new CatalogSearchReindexBatchService(
                        catalogItemRepository, branchItemRepository, outboxEventService);

        CatalogSearchReindexBatchService.BatchResult result =
                service.enqueueCatalogItemBatch(null, PageRequest.of(0, 100));

        ArgumentCaptor<Object> data = ArgumentCaptor.forClass(Object.class);
        verify(outboxEventService)
                .enqueue(
                        eq(CatalogEventType.CATALOG_ITEM_UPSERTED),
                        eq("CATALOG_ITEM"),
                        eq(item.getId()),
                        data.capture());
        assertThat(data.getValue())
                .isInstanceOfSatisfying(
                        java.util.Map.class,
                        payload -> {
                            assertThat(payload).containsEntry("itemId", item.getId());
                            assertThat(payload).containsEntry("status", CatalogStatus.ACTIVE);
                        });
        assertThat(result.queued()).isEqualTo(1);
        assertThat(result.lastProcessedId()).isEqualTo(item.getId());
    }

    @Test
    void branchItemBatchWritesCurrentSnapshotToOutbox() {
        CatalogItemRepository catalogItemRepository = Mockito.mock(CatalogItemRepository.class);
        BranchItemRepository branchItemRepository = Mockito.mock(BranchItemRepository.class);
        OutboxEventService outboxEventService = Mockito.mock(OutboxEventService.class);
        CatalogItem item = catalogItem();
        BranchItem branchItem = new BranchItem();
        branchItem.setId(UUID.randomUUID());
        branchItem.setItem(item);
        branchItem.setBranchId(UUID.randomUUID());
        branchItem.setSellingPrice(BigDecimal.valueOf(53000));
        branchItem.setIsAvailable(true);
        when(branchItemRepository.findSnapshotIds(any(PageRequest.class)))
                .thenReturn(List.of(branchItem.getId()));
        when(branchItemRepository.findAllByIdInForUpdate(List.of(branchItem.getId())))
                .thenReturn(List.of(branchItem));
        CatalogSearchReindexBatchService service =
                new CatalogSearchReindexBatchService(
                        catalogItemRepository, branchItemRepository, outboxEventService);

        service.enqueueBranchItemBatch(null, PageRequest.of(0, 100));

        verify(outboxEventService)
                .enqueue(
                        eq(CatalogEventType.BRANCH_ITEM_UPSERTED),
                        eq("BRANCH_ITEM"),
                        eq(branchItem.getId()),
                        any());
    }

    private CatalogItem catalogItem() {
        CatalogItem item = new CatalogItem();
        item.setId(UUID.randomUUID());
        item.setRestaurantId(UUID.randomUUID());
        item.setName("Pho Bo");
        item.setItemType(CatalogItemType.FOOD);
        item.setBasePrice(BigDecimal.valueOf(50000));
        item.setCurrency("VND");
        item.setIsVegetarian(false);
        item.setStatus(CatalogStatus.ACTIVE);
        return item;
    }
}
