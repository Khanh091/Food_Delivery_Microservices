package com.khanh.fooddelivery.restaurant_service.service;

import com.khanh.fooddelivery.restaurant_service.dto.response.BranchOperatingStatusResponse;
import java.time.ZonedDateTime;
import java.util.UUID;

public interface BranchOperatingStatusService {
    BranchOperatingStatusResponse getOperatingStatus(UUID branchId, ZonedDateTime now);
}
