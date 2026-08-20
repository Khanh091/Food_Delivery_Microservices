package com.khanh.fooddelivery.order_service.dto.request;

import jakarta.validation.constraints.Size;

public record RejectOrderRequest(@Size(max = 500) String reason) {}
