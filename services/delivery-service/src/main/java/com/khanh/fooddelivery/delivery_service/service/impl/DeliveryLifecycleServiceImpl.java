package com.khanh.fooddelivery.delivery_service.service.impl;

import com.khanh.fooddelivery.delivery_service.client.DriverServiceClient;
import com.khanh.fooddelivery.delivery_service.client.OrderServiceClient;
import com.khanh.fooddelivery.delivery_service.dto.request.DeliveryMatchingRequest;
import com.khanh.fooddelivery.delivery_service.dto.response.DeliveryOfferResponse;
import com.khanh.fooddelivery.delivery_service.dto.response.DeliveryResponse;
import com.khanh.fooddelivery.delivery_service.mapper.DeliveryMapper;
import com.khanh.fooddelivery.delivery_service.model.Delivery;
import com.khanh.fooddelivery.delivery_service.model.DeliveryStatus;
import com.khanh.fooddelivery.delivery_service.repository.DeliveryRepository;
import com.khanh.fooddelivery.delivery_service.security.CurrentBearerTokenProvider;
import com.khanh.fooddelivery.delivery_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.delivery_service.service.DeliveryLifecycleService;
import com.khanh.fooddelivery.delivery_service.service.DeliveryMatchingService;
import com.khanh.fooddelivery.delivery_service.service.DeliveryOfferService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryLifecycleServiceImpl implements DeliveryLifecycleService {

    private final DeliveryRepository deliveries;
    private final CurrentUserProvider users;
    private final CurrentBearerTokenProvider bearer;
    private final OrderServiceClient orders;
    private final DriverServiceClient drivers;
    private final DeliveryMatchingService matching;
    private final DeliveryOfferService offerService;
    private final DeliveryMapper mapper;

    @Override
    public DeliveryResponse startMatching(DeliveryMatchingRequest request) {
        return matching.startMatching(request);
    }

    @Override
    public List<DeliveryOfferResponse> offers(Jwt jwt) {
        return offerService.offers(jwt);
    }

    @Override
    public DeliveryResponse accept(Jwt jwt, UUID deliveryId) {
        return offerService.accept(jwt, deliveryId);
    }

    @Override
    public void reject(Jwt jwt, UUID deliveryId) {
        offerService.reject(jwt, deliveryId);
    }

    @Override
    public DeliveryResponse pickup(Jwt jwt, UUID deliveryId) {
        Delivery delivery = mine(jwt, deliveryId);
        if (delivery.getStatus() != DeliveryStatus.ASSIGNED) {
            throw new IllegalStateException("Delivery cannot be picked up");
        }
        delivery.setStatus(DeliveryStatus.PICKED_UP);
        orders.pickedUp(bearer.getBearerToken(), delivery.getOrderId());
        return mapper.toResponse(delivery);
    }

    @Override
    public DeliveryResponse delivered(Jwt jwt, UUID deliveryId) {
        Delivery delivery = mine(jwt, deliveryId);
        if (delivery.getStatus() != DeliveryStatus.PICKED_UP) {
            throw new IllegalStateException("Delivery cannot be completed");
        }
        delivery.setStatus(DeliveryStatus.DELIVERED);
        orders.delivered(bearer.getBearerToken(), delivery.getOrderId());
        drivers.release(bearer.getBearerToken(), delivery.getDriverId(), deliveryId);
        return mapper.toResponse(delivery);
    }

    @Override
    public void expireOffers() {
        offerService.expireOffers();
    }

    private Delivery mine(Jwt jwt, UUID deliveryId) {
        Delivery delivery = deliveries.findByIdForUpdate(deliveryId).orElseThrow();
        if (!users.getCurrentUserId(jwt).equals(delivery.getDriverId())) {
            throw new IllegalStateException("Not assigned driver");
        }
        return delivery;
    }
}
