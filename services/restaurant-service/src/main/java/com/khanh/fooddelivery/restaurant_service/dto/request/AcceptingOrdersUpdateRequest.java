package com.khanh.fooddelivery.restaurant_service.dto.request;

import jakarta.validation.constraints.NotNull;

public record AcceptingOrdersUpdateRequest(@NotNull Boolean acceptingOrders) {}
