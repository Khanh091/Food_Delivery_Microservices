package com.khanh.fooddelivery.catalog_service.service.impl;

import com.khanh.fooddelivery.catalog_service.entity.BranchItem;
import com.khanh.fooddelivery.catalog_service.entity.CatalogItem;
import com.khanh.fooddelivery.catalog_service.outbox.CatalogEventType;
import com.khanh.fooddelivery.catalog_service.outbox.CatalogItemSearchEventPublisher;
import com.khanh.fooddelivery.catalog_service.outbox.OutboxEventService;
import com.khanh.fooddelivery.catalog_service.repository.BranchItemRepository;
import com.khanh.fooddelivery.catalog_service.repository.CatalogItemRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogSearchReindexBatchService {
    private final CatalogItemRepository catalogItemRepository;
    private final BranchItemRepository branchItemRepository;
    private final CatalogItemSearchEventPublisher catalogItemSearchEventPublisher;
    private final OutboxEventService outboxEventService;

    public CatalogSearchReindexBatchService(
            CatalogItemRepository catalogItemRepository,
            BranchItemRepository branchItemRepository,
            CatalogItemSearchEventPublisher catalogItemSearchEventPublisher,
            OutboxEventService outboxEventService) {
        this.catalogItemRepository = catalogItemRepository;
        this.branchItemRepository = branchItemRepository;
        this.catalogItemSearchEventPublisher = catalogItemSearchEventPublisher;
        this.outboxEventService = outboxEventService;
    }

    @Transactional
    public BatchResult enqueueCatalogItemBatch(UUID afterId, Pageable pageable) {
        List<UUID> itemIds = snapshotIds(afterId, pageable, catalogItemRepository);
        if (itemIds.isEmpty()) {
            return new BatchResult(0, null);
        }
        List<CatalogItem> items = catalogItemRepository.findAllByIdInForUpdate(itemIds);
        catalogItemSearchEventPublisher.enqueueAll(
                CatalogEventType.CATALOG_ITEM_UPSERTED, items, "SEARCH_REINDEX");
        return new BatchResult(items.size(), lastId(itemIds));
    }

    @Transactional
    public BatchResult enqueueBranchItemBatch(UUID afterId, Pageable pageable) {
        List<UUID> branchItemIds = snapshotIds(afterId, pageable, branchItemRepository);
        if (branchItemIds.isEmpty()) {
            return new BatchResult(0, null);
        }
        List<BranchItem> items = branchItemRepository.findAllByIdInForUpdate(branchItemIds);
        items.forEach(
                item ->
                        outboxEventService.enqueue(
                                CatalogEventType.BRANCH_ITEM_UPSERTED,
                                "BRANCH_ITEM",
                                item.getId(),
                                com.khanh.fooddelivery.catalog_service.outbox.CatalogEventData.branchItem(item, "SEARCH_REINDEX")));
        return new BatchResult(items.size(), lastId(branchItemIds));
    }

    private List<UUID> snapshotIds(
            UUID afterId, Pageable pageable, CatalogItemRepository repository) {
        return afterId == null
                ? repository.findSnapshotIds(pageable)
                : repository.findSnapshotIdsAfter(afterId, pageable);
    }

    private List<UUID> snapshotIds(
            UUID afterId, Pageable pageable, BranchItemRepository repository) {
        return afterId == null
                ? repository.findSnapshotIds(pageable)
                : repository.findSnapshotIdsAfter(afterId, pageable);
    }

    private UUID lastId(List<UUID> ids) {
        return ids.isEmpty() ? null : ids.getLast();
    }

    public record BatchResult(int queued, UUID lastProcessedId) {}
}
