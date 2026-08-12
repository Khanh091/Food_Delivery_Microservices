package com.khanh.fooddelivery.search_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.search_service.client.CatalogSearchReindexClient;
import com.khanh.fooddelivery.search_service.client.CatalogSearchReindexClient.CatalogReindexResponse;
import com.khanh.fooddelivery.search_service.client.CatalogSearchReindexClient.CatalogSnapshotResult;
import com.khanh.fooddelivery.search_service.exception.SearchApiException;
import com.khanh.fooddelivery.search_service.exception.SearchErrorCode;
import com.khanh.fooddelivery.search_service.repository.SearchProjectionRepository;
import com.khanh.fooddelivery.search_service.security.CurrentBearerTokenProvider;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CatalogSearchRebuildServiceImplTests {
    @Test
    void recreatesIndexBeforeTriggeringCatalogSnapshot() {
        SearchProjectionRepository repository = Mockito.mock(SearchProjectionRepository.class);
        CatalogSearchReindexClient client = Mockito.mock(CatalogSearchReindexClient.class);
        CurrentBearerTokenProvider bearerTokenProvider = Mockito.mock(CurrentBearerTokenProvider.class);
        when(bearerTokenProvider.getBearerToken()).thenReturn("Bearer token");
        when(client.triggerSearchReindex("Bearer token"))
                .thenReturn(successResponse(5, 8));
        CatalogSearchRebuildService service =
                new CatalogSearchRebuildServiceImpl(repository, client, bearerTokenProvider);

        CatalogSnapshotResult result = service.rebuild();

        assertThat(result.catalogItemsQueued()).isEqualTo(5);
        assertThat(result.branchItemsQueued()).isEqualTo(8);
        org.mockito.Mockito.inOrder(repository, client)
                .verify(repository)
                .recreateIndex();
        verify(client).triggerSearchReindex("Bearer token");
    }

    @Test
    void indexRecreateFailureDoesNotCallCatalog() {
        SearchProjectionRepository repository = Mockito.mock(SearchProjectionRepository.class);
        CatalogSearchReindexClient client = Mockito.mock(CatalogSearchReindexClient.class);
        CurrentBearerTokenProvider bearerTokenProvider = Mockito.mock(CurrentBearerTokenProvider.class);
        doThrow(new IllegalStateException("Elasticsearch unavailable")).when(repository).recreateIndex();
        CatalogSearchRebuildService service =
                new CatalogSearchRebuildServiceImpl(repository, client, bearerTokenProvider);

        assertThatThrownBy(service::rebuild)
                .isInstanceOf(SearchApiException.class)
                .extracting(exception -> ((SearchApiException) exception).getErrorCode())
                .isEqualTo(SearchErrorCode.INDEX_RECREATE_FAILED);
        org.mockito.Mockito.verifyNoInteractions(client);
    }

    @Test
    void catalogTriggerFailureIsStandardized() {
        SearchProjectionRepository repository = Mockito.mock(SearchProjectionRepository.class);
        CatalogSearchReindexClient client = Mockito.mock(CatalogSearchReindexClient.class);
        CurrentBearerTokenProvider bearerTokenProvider = Mockito.mock(CurrentBearerTokenProvider.class);
        when(bearerTokenProvider.getBearerToken()).thenReturn("Bearer token");
        doThrow(new IllegalStateException("catalog unavailable"))
                .when(client)
                .triggerSearchReindex("Bearer token");
        CatalogSearchRebuildService service =
                new CatalogSearchRebuildServiceImpl(repository, client, bearerTokenProvider);

        assertThatThrownBy(service::rebuild)
                .isInstanceOf(SearchApiException.class)
                .extracting(exception -> ((SearchApiException) exception).getErrorCode())
                .isEqualTo(SearchErrorCode.CATALOG_REINDEX_TRIGGER_FAILED);
    }

    @Test
    void concurrentRebuildIsRejected() throws Exception {
        SearchProjectionRepository repository = Mockito.mock(SearchProjectionRepository.class);
        CatalogSearchReindexClient client = Mockito.mock(CatalogSearchReindexClient.class);
        CurrentBearerTokenProvider bearerTokenProvider = Mockito.mock(CurrentBearerTokenProvider.class);
        CountDownLatch recreateStarted = new CountDownLatch(1);
        CountDownLatch releaseRecreate = new CountDownLatch(1);
        Mockito.doAnswer(
                        ignored -> {
                            recreateStarted.countDown();
                            releaseRecreate.await(5, TimeUnit.SECONDS);
                            return null;
                        })
                .when(repository)
                .recreateIndex();
        when(bearerTokenProvider.getBearerToken()).thenReturn("Bearer token");
        when(client.triggerSearchReindex("Bearer token")).thenReturn(successResponse(0, 0));
        CatalogSearchRebuildService service =
                new CatalogSearchRebuildServiceImpl(repository, client, bearerTokenProvider);

        CompletableFuture<Void> first = CompletableFuture.runAsync(service::rebuild);
        assertThat(recreateStarted.await(5, TimeUnit.SECONDS)).isTrue();
        assertThatThrownBy(service::rebuild)
                .isInstanceOf(SearchApiException.class)
                .extracting(exception -> ((SearchApiException) exception).getErrorCode())
                .isEqualTo(SearchErrorCode.REBUILD_IN_PROGRESS);
        releaseRecreate.countDown();
        first.get(5, TimeUnit.SECONDS);
    }

    private CatalogReindexResponse successResponse(long catalogItems, long branchItems) {
        return new CatalogReindexResponse(
                true,
                "SUCCESS",
                "Catalog search snapshot queued",
                new CatalogSnapshotResult(catalogItems, branchItems),
                Instant.now());
    }
}
