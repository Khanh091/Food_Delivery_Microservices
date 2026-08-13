package com.khanh.fooddelivery.restaurant_service.dto.response;

import java.time.LocalTime;

public record PublicBranchBusinessHourResponse(
        short dayOfWeek, LocalTime openTime, LocalTime closeTime, boolean closed) {}
