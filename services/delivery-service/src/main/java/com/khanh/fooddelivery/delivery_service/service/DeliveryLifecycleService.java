package com.khanh.fooddelivery.delivery_service.service;

import com.khanh.fooddelivery.delivery_service.dto.response.*;
import java.util.*;
import org.springframework.security.oauth2.jwt.Jwt;

public interface DeliveryLifecycleService {
    List<DeliveryOfferResponse> offers(Jwt jwt);
    Optional<CurrentDeliveryOfferResponse> currentOffer(Jwt jwt);
    Optional<DeliveryResponse> currentActiveDelivery(Jwt jwt);
    DeliveryResponse accept(Jwt jwt, UUID deliveryId);
    void reject(Jwt jwt, UUID deliveryId);
    DeliveryResponse pickup(Jwt jwt, UUID deliveryId);
    DeliveryResponse delivered(Jwt jwt, UUID deliveryId);
    DeliveryResponse confirmRestaurantPayment(Jwt jwt, UUID deliveryId);
    DeliveryResponse collectCash(Jwt jwt, UUID deliveryId);
    void expireOffers();
}
