package com.khanh.fooddelivery.search_service.service;

import com.khanh.fooddelivery.search_service.client.RestaurantSearchReindexClient;
import com.khanh.fooddelivery.search_service.client.RestaurantSearchReindexClient.RestaurantSnapshotResult;
import com.khanh.fooddelivery.search_service.exception.SearchApiException;
import com.khanh.fooddelivery.search_service.exception.SearchErrorCode;
import com.khanh.fooddelivery.search_service.repository.RestaurantSearchProjectionRepository;
import com.khanh.fooddelivery.search_service.security.CurrentBearerTokenProvider;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Service;

@Service
public class RestaurantSearchRebuildServiceImpl implements RestaurantSearchRebuildService {
    private final RestaurantSearchProjectionRepository repository; private final RestaurantSearchReindexClient client; private final CurrentBearerTokenProvider bearer;
    private final AtomicBoolean rebuilding = new AtomicBoolean();
    public RestaurantSearchRebuildServiceImpl(RestaurantSearchProjectionRepository repository, RestaurantSearchReindexClient client, CurrentBearerTokenProvider bearer) {this.repository=repository;this.client=client;this.bearer=bearer;}
    public RestaurantSnapshotResult rebuild() {
        if(!rebuilding.compareAndSet(false,true)) throw new SearchApiException(SearchErrorCode.REBUILD_IN_PROGRESS);
        try { try{repository.recreateIndex();}catch(RuntimeException e){throw new SearchApiException(SearchErrorCode.INDEX_RECREATE_FAILED,e);} try{return client.triggerSearchReindex(bearer.getBearerToken());}catch(RuntimeException e){throw new SearchApiException(SearchErrorCode.RESTAURANT_REINDEX_TRIGGER_FAILED,e);} }
        finally { rebuilding.set(false); }
    }
}
