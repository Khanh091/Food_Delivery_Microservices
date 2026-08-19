package com.khanh.fooddelivery.catalog_service.repository.projection;

import java.util.UUID;

public interface ItemPlacementCount {
    UUID getItemId();

    long getPlacementCount();
}
