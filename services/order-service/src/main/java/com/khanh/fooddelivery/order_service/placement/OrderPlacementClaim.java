package com.khanh.fooddelivery.order_service.placement;

import java.util.UUID;

public record OrderPlacementClaim(
        UUID requestId,
        UUID reservedOrderId,
        UUID branchId,
        long cartVersion,
        UUID claimToken,
        Status status
) {

    public enum Status {
        ACTIVE,
        COMPLETED,
        IN_PROGRESS
    }
}
