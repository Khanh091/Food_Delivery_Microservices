package com.khanh.fooddelivery.order_service.client.dto.response;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
@JsonIgnoreProperties(ignoreUnknown = true)
public record InternalCartSnapshotResponse(
        UUID ownerUserId,
        UUID restaurantId,
        UUID branchId,
        String currency,
        List<InternalCartItemSnapshotResponse> items,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
}
