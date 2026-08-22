package com.khanh.fooddelivery.delivery_service.outbox;

import com.khanh.fooddelivery.delivery_service.model.DeliveryOffer;

public interface DeliveryOutboxService {

    void publishDeliveryOfferCreated(DeliveryOffer offer);
}
