package com.khanh.fooddelivery.search_service.exception;

import java.time.Instant;

public record SearchErrorResponse(String code, String message, String path, Instant timestamp) {}
