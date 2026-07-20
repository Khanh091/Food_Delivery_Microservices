package com.khanh.fooddelivery.restaurant_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BranchBusinessHoursUpdateRequest(
        @NotNull @Size(max = 7) List<@Valid BranchBusinessHourRequest> hours) {}
