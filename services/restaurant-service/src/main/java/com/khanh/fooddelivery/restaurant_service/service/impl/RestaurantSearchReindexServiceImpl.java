package com.khanh.fooddelivery.restaurant_service.service.impl;

import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantSearchReindexResponse;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantSearchReindexProperties;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantSearchReindexService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(RestaurantSearchReindexProperties.class)
public class RestaurantSearchReindexServiceImpl
        implements RestaurantSearchReindexService {

    private final RestaurantSearchReindexBatchService batches;
    private final RestaurantSearchReindexProperties properties;

    @Override
    public RestaurantSearchReindexResponse enqueueCurrentRestaurantSnapshot() {
        if (properties.getBatchSize() < 1) {
            throw new IllegalStateException(
                    "Restaurant search reindex batch size must be positive"
            );
        }

        return new RestaurantSearchReindexResponse(
                enqueue(batches::enqueueRestaurantBatch),
                enqueue(batches::enqueueBranchBatch)
        );
    }

    private long enqueue(BatchEnqueuer enqueuer) {
        long total = 0;
        UUID cursor = null;
        RestaurantSearchReindexBatchService.BatchResult result;

        do {
            result = enqueuer.enqueue(
                    cursor,
                    PageRequest.of(
                            0,
                            properties.getBatchSize(),
                            Sort.by("id")
                    )
            );

            total += result.queued();
            cursor = result.lastProcessedId();
        } while (cursor != null);

        return total;
    }

    @FunctionalInterface
    private interface BatchEnqueuer {

        RestaurantSearchReindexBatchService.BatchResult enqueue(
                UUID cursor,
                org.springframework.data.domain.Pageable page
        );
    }
}