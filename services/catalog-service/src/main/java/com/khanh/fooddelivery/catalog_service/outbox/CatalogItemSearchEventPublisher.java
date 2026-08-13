package com.khanh.fooddelivery.catalog_service.outbox;

import com.khanh.fooddelivery.catalog_service.entity.CatalogItem;
import com.khanh.fooddelivery.catalog_service.repository.ItemImageRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Builds the current customer-facing CatalogItem snapshot for the existing catalog event contract.
 * The caller owns the surrounding business transaction; {@link OutboxEventService} enforces that
 * one is active before it persists the outbox event.
 */
@Service
@RequiredArgsConstructor
public class CatalogItemSearchEventPublisher {
    private final ItemImageRepository imageRepository;
    private final OutboxEventService outboxEventService;

    public void enqueue(CatalogEventType eventType, CatalogItem item, String action) {
        enqueue(eventType, item, action, primaryImageUrl(item.getId()));
    }

    public void enqueueAll(CatalogEventType eventType, Collection<CatalogItem> items, String action) {
        if (items.isEmpty()) {
            return;
        }
        Map<UUID, String> primaryImages = primaryImageUrls(items.stream().map(CatalogItem::getId).toList());
        items.forEach(item -> enqueue(eventType, item, action, primaryImages.get(item.getId())));
    }

    private void enqueue(
            CatalogEventType eventType, CatalogItem item, String action, String primaryImageUrl) {
        outboxEventService.enqueue(
                eventType,
                "CATALOG_ITEM",
                item.getId(),
                CatalogEventData.catalogItem(item, action, primaryImageUrl));
    }

    private String primaryImageUrl(UUID itemId) {
        return imageRepository
                .findFirstByItemIdAndIsPrimaryTrue(itemId)
                .map(image -> image.getImageUrl())
                .orElse(null);
    }

    private Map<UUID, String> primaryImageUrls(Collection<UUID> itemIds) {
        Map<UUID, String> results = new HashMap<>();
        imageRepository
                .findAllByItemIdInOrderByIsPrimaryDescSortOrderAscCreatedAtAsc(List.copyOf(itemIds))
                .stream()
                .filter(image -> Boolean.TRUE.equals(image.getIsPrimary()))
                .forEach(image -> results.putIfAbsent(image.getItem().getId(), image.getImageUrl()));
        return results;
    }
}
