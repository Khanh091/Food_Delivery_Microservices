package com.khanh.fooddelivery.catalog_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.catalog_service.dto.response.CatalogSearchReindexResponse;
import com.khanh.fooddelivery.catalog_service.service.CatalogSearchReindexProperties;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CatalogSearchReindexServiceImplTests {
    @Test
    void enqueuesCatalogAndBranchSnapshotsInBatches() {
        CatalogSearchReindexBatchService batchService =
                Mockito.mock(CatalogSearchReindexBatchService.class);
        when(batchService.enqueueCatalogItemBatch(nullable(java.util.UUID.class), any()))
                .thenReturn(
                        new CatalogSearchReindexBatchService.BatchResult(2, java.util.UUID.randomUUID()),
                        new CatalogSearchReindexBatchService.BatchResult(1, null));
        when(batchService.enqueueBranchItemBatch(nullable(java.util.UUID.class), any()))
                .thenReturn(
                        new CatalogSearchReindexBatchService.BatchResult(2, java.util.UUID.randomUUID()),
                        new CatalogSearchReindexBatchService.BatchResult(2, null));
        CatalogSearchReindexProperties properties = new CatalogSearchReindexProperties();
        properties.setBatchSize(2);
        CatalogSearchReindexServiceImpl service =
                new CatalogSearchReindexServiceImpl(batchService, properties);

        CatalogSearchReindexResponse response = service.enqueueCurrentCatalogSnapshot();

        assertThat(response.catalogItemsQueued()).isEqualTo(3);
        assertThat(response.branchItemsQueued()).isEqualTo(4);
        verify(batchService, times(2)).enqueueCatalogItemBatch(any(), any());
        verify(batchService, times(2)).enqueueBranchItemBatch(any(), any());
    }
}
