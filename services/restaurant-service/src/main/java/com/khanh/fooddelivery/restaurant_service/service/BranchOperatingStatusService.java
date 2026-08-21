package com.khanh.fooddelivery.restaurant_service.service;

import com.khanh.fooddelivery.restaurant_service.dto.response.BranchOperatingStatusResponse;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

public interface BranchOperatingStatusService {
    ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    BranchOperatingStatusResponse getOperatingStatus(UUID branchId, ZonedDateTime now);
}
