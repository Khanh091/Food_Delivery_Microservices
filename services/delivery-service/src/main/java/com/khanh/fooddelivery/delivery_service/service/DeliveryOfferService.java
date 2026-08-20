package com.khanh.fooddelivery.delivery_service.service;

import com.khanh.fooddelivery.delivery_service.dto.response.DeliveryOfferResponse;
import com.khanh.fooddelivery.delivery_service.dto.response.DeliveryResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public interface DeliveryOfferService {

    List<DeliveryOfferResponse> offers(Jwt jwt);

    DeliveryResponse accept(Jwt jwt, UUID deliveryId);

    void reject(Jwt jwt, UUID deliveryId);

    void expireOffers();
}
