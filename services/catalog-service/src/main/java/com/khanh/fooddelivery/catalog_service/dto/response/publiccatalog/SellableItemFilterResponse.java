package com.khanh.fooddelivery.catalog_service.dto.response.publiccatalog;

import java.util.List;
import java.util.UUID;

public record SellableItemFilterResponse(List<UUID> itemIds) {}
