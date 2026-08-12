package com.khanh.fooddelivery.search_service.service;

import com.khanh.fooddelivery.search_service.client.CatalogSearchReindexClient;
import com.khanh.fooddelivery.search_service.client.CatalogSearchReindexClient.CatalogReindexResponse;
import com.khanh.fooddelivery.search_service.client.CatalogSearchReindexClient.CatalogSnapshotResult;
import com.khanh.fooddelivery.search_service.exception.SearchApiException;
import com.khanh.fooddelivery.search_service.exception.SearchErrorCode;
import com.khanh.fooddelivery.search_service.repository.SearchProjectionRepository;
import com.khanh.fooddelivery.search_service.security.CurrentBearerTokenProvider;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Service;

@Service
public class CatalogSearchRebuildServiceImpl implements CatalogSearchRebuildService {
    private final SearchProjectionRepository projectionRepository;
    private final CatalogSearchReindexClient catalogClient;
    private final CurrentBearerTokenProvider bearerTokenProvider;
    private final AtomicBoolean rebuildRunning = new AtomicBoolean(false);

    public CatalogSearchRebuildServiceImpl(
            SearchProjectionRepository projectionRepository,
            CatalogSearchReindexClient catalogClient,
            CurrentBearerTokenProvider bearerTokenProvider) {
        this.projectionRepository = projectionRepository;
        this.catalogClient = catalogClient;
        this.bearerTokenProvider = bearerTokenProvider;
    }

    @Override
    public CatalogSnapshotResult rebuild() {
        if (!rebuildRunning.compareAndSet(false, true)) {
            throw new SearchApiException(SearchErrorCode.REBUILD_IN_PROGRESS);
        }
        try {
            recreateIndex();
            return triggerCatalogSnapshot();
        } finally {
            rebuildRunning.set(false);
        }
    }

    private void recreateIndex() {
        try {
            projectionRepository.recreateIndex();
        } catch (RuntimeException exception) {
            throw new SearchApiException(SearchErrorCode.INDEX_RECREATE_FAILED, exception);
        }
    }

    private CatalogSnapshotResult triggerCatalogSnapshot() {
        try {
            CatalogReindexResponse response =
                    catalogClient.triggerSearchReindex(bearerTokenProvider.getBearerToken());
            if (!response.success() || response.data() == null) {
                throw new SearchApiException(SearchErrorCode.CATALOG_REINDEX_TRIGGER_FAILED);
            }
            return response.data();
        } catch (SearchApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new SearchApiException(SearchErrorCode.CATALOG_REINDEX_TRIGGER_FAILED, exception);
        }
    }
}
