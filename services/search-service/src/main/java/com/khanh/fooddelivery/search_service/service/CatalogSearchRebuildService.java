package com.khanh.fooddelivery.search_service.service;

import com.khanh.fooddelivery.search_service.client.CatalogSearchReindexClient.CatalogSnapshotResult;

public interface CatalogSearchRebuildService {
    CatalogSnapshotResult rebuild();
}
