package com.khanh.fooddelivery.order_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RemoteApiResponse<T>(boolean success, String code, String message, T data, Instant timestamp) {}
