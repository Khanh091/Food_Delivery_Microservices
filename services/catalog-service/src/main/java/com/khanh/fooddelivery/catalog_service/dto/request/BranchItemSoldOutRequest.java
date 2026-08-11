package com.khanh.fooddelivery.catalog_service.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record BranchItemSoldOutRequest(@NotNull @Future Instant soldOutUntil) {}
