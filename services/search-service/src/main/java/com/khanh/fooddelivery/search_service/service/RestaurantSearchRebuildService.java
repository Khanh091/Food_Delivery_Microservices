package com.khanh.fooddelivery.search_service.service;

import com.khanh.fooddelivery.search_service.client.RestaurantSearchReindexClient.RestaurantSnapshotResult;
public interface RestaurantSearchRebuildService { RestaurantSnapshotResult rebuild(); }
