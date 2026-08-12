package com.khanh.fooddelivery.catalog_service.service.impl;

import com.khanh.fooddelivery.catalog_service.dto.response.CatalogSearchReindexResponse;
import com.khanh.fooddelivery.catalog_service.service.CatalogSearchReindexProperties;
import com.khanh.fooddelivery.catalog_service.service.CatalogSearchReindexService;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class CatalogSearchReindexServiceImpl implements CatalogSearchReindexService {
    private static final Sort SNAPSHOT_SORT = Sort.by(Sort.Direction.ASC, "id");

    private final CatalogSearchReindexBatchService batchService;
    private final CatalogSearchReindexProperties properties;

    public CatalogSearchReindexServiceImpl(
            CatalogSearchReindexBatchService batchService, CatalogSearchReindexProperties properties) {
        this.batchService = batchService;
        this.properties = properties;
    }

    @Override
    public CatalogSearchReindexResponse enqueueCurrentCatalogSnapshot() {
        validateBatchSize();
        long catalogItemsQueued = enqueueCatalogItems();
        long branchItemsQueued = enqueueBranchItems();
        return new CatalogSearchReindexResponse(catalogItemsQueued, branchItemsQueued);
    }

    private long enqueueCatalogItems() {
        return enqueueBatches(batchService::enqueueCatalogItemBatch);
    }

    private long enqueueBranchItems() {
        return enqueueBatches(batchService::enqueueBranchItemBatch);
    }

    private long enqueueBatches(BatchEnqueuer enqueuer) {
        UUID cursor = null;
        long queued = 0;
        CatalogSearchReindexBatchService.BatchResult result;
        do {
            result =
                    enqueuer.enqueue(
                            cursor, PageRequest.of(0, properties.getBatchSize(), SNAPSHOT_SORT));
            queued += result.queued();
            cursor = result.lastProcessedId();
        } while (cursor != null);
        return queued;
    }

    private void validateBatchSize() {
        if (properties.getBatchSize() < 1) {
            throw new IllegalStateException("Catalog search reindex batch size must be positive");
        }
    }

    @FunctionalInterface
    private interface BatchEnqueuer {
        CatalogSearchReindexBatchService.BatchResult enqueue(
                UUID afterId, org.springframework.data.domain.Pageable pageable);
    }
}
