package com.khanh.fooddelivery.delivery_service.service;

import com.khanh.fooddelivery.delivery_service.dto.request.DeliveryMatchingRequest;
import com.khanh.fooddelivery.delivery_service.dto.response.DeliveryResponse;
import com.khanh.fooddelivery.delivery_service.model.Delivery;

public interface DeliveryMatchingService {

    DeliveryResponse startMatching(DeliveryMatchingRequest request);

    void offerNext(Delivery delivery);
}
