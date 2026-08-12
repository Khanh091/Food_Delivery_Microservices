package com.khanh.fooddelivery.restaurant_service.service.impl;

import com.khanh.fooddelivery.restaurant_service.entity.Restaurant;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantBranch;
import com.khanh.fooddelivery.restaurant_service.outbox.OutboxEventService;
import com.khanh.fooddelivery.restaurant_service.outbox.RestaurantEventData;
import com.khanh.fooddelivery.restaurant_service.outbox.RestaurantEventType;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantBranchRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestaurantSearchReindexBatchService {
    private final RestaurantRepository restaurants;
    private final RestaurantBranchRepository branches;
    private final OutboxEventService outbox;

    @Transactional
    public BatchResult enqueueRestaurantBatch(UUID afterId, Pageable page) {
        List<UUID> ids = restaurants.findSnapshotIds(afterId, page);
        if (ids.isEmpty()) return new BatchResult(0, null);
        List<Restaurant> locked = restaurants.findAllByIdInForUpdate(ids);
        locked.forEach(restaurant -> outbox.enqueue(RestaurantEventType.RESTAURANT_UPSERTED, "RESTAURANT", restaurant.getId(), RestaurantEventData.restaurant(restaurant, "SEARCH_REINDEX")));
        return new BatchResult(locked.size(), ids.getLast());
    }

    @Transactional
    public BatchResult enqueueBranchBatch(UUID afterId, Pageable page) {
        List<UUID> ids = branches.findSnapshotIds(afterId, page);
        if (ids.isEmpty()) return new BatchResult(0, null);
        List<RestaurantBranch> locked = branches.findAllByIdInForUpdate(ids);
        locked.forEach(branch -> outbox.enqueue(RestaurantEventType.RESTAURANT_BRANCH_UPSERTED, "RESTAURANT_BRANCH", branch.getId(), RestaurantEventData.branch(branch, "SEARCH_REINDEX")));
        return new BatchResult(locked.size(), ids.getLast());
    }
    public record BatchResult(int queued, UUID lastProcessedId) {}
}
