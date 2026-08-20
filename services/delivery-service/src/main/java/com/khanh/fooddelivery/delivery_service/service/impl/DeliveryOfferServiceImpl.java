package com.khanh.fooddelivery.delivery_service.service.impl;

import com.khanh.fooddelivery.delivery_service.client.DriverServiceClient;
import com.khanh.fooddelivery.delivery_service.client.OrderServiceClient;
import com.khanh.fooddelivery.delivery_service.dto.response.DeliveryOfferResponse;
import com.khanh.fooddelivery.delivery_service.dto.response.DeliveryResponse;
import com.khanh.fooddelivery.delivery_service.mapper.DeliveryMapper;
import com.khanh.fooddelivery.delivery_service.model.Delivery;
import com.khanh.fooddelivery.delivery_service.model.DeliveryOffer;
import com.khanh.fooddelivery.delivery_service.model.DeliveryOfferStatus;
import com.khanh.fooddelivery.delivery_service.model.DeliveryStatus;
import com.khanh.fooddelivery.delivery_service.repository.DeliveryOfferRepository;
import com.khanh.fooddelivery.delivery_service.repository.DeliveryRepository;
import com.khanh.fooddelivery.delivery_service.security.CurrentBearerTokenProvider;
import com.khanh.fooddelivery.delivery_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.delivery_service.service.DeliveryMatchingService;
import com.khanh.fooddelivery.delivery_service.service.DeliveryOfferService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryOfferServiceImpl implements DeliveryOfferService {

    private final DeliveryRepository deliveries;
    private final DeliveryOfferRepository offers;
    private final CurrentUserProvider users;
    private final CurrentBearerTokenProvider bearer;
    private final OrderServiceClient orders;
    private final DriverServiceClient drivers;
    private final DeliveryMatchingService matching;
    private final DeliveryMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryOfferResponse> offers(Jwt jwt) {
        UUID driverId = users.getCurrentUserId(jwt);
        return offers.findAll().stream()
                .filter(offer -> offer.getDriverId().equals(driverId))
                .filter(offer -> offer.getStatus() == DeliveryOfferStatus.PENDING)
                .filter(offer -> offer.getExpiresAt().isAfter(Instant.now()))
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public DeliveryResponse accept(Jwt jwt, UUID deliveryId) {
        UUID driverId = users.getCurrentUserId(jwt);
        Delivery delivery = deliveries.findByIdForUpdate(deliveryId).orElseThrow();
        DeliveryOffer offer = offers.findByDeliveryIdAndDriverIdAndStatus(
                deliveryId,
                driverId,
                DeliveryOfferStatus.PENDING
        ).orElseThrow();

        if (delivery.getStatus() != DeliveryStatus.MATCHING
                || !offer.getExpiresAt().isAfter(Instant.now())) {
            throw new IllegalStateException("Delivery offer is no longer active");
        }

        drivers.acceptOffer(bearer.getBearerToken(), driverId, deliveryId);
        offer.setStatus(DeliveryOfferStatus.ACCEPTED);
        offer.setRespondedAt(Instant.now());
        delivery.setDriverId(driverId);
        delivery.setStatus(DeliveryStatus.ASSIGNED);
        orders.assigned(bearer.getBearerToken(), delivery.getOrderId());
        return mapper.toResponse(delivery);
    }

    @Override
    public void reject(Jwt jwt, UUID deliveryId) {
        UUID driverId = users.getCurrentUserId(jwt);
        DeliveryOffer offer = offers.findByDeliveryIdAndDriverIdAndStatus(
                deliveryId,
                driverId,
                DeliveryOfferStatus.PENDING
        ).orElseThrow();
        offer.setStatus(DeliveryOfferStatus.REJECTED);
        offer.setRespondedAt(Instant.now());
        drivers.releaseOffer(bearer.getBearerToken(), driverId, deliveryId);
        deliveries.findByIdForUpdate(deliveryId).ifPresent(matching::offerNext);
    }

    @Override
    @Scheduled(fixedDelay = 30000)
    public void expireOffers() {
        for (DeliveryOffer offer : offers.findExpired(Instant.now())) {
            offer.setStatus(DeliveryOfferStatus.EXPIRED);
            offer.setRespondedAt(Instant.now());
            drivers.releaseOffer(
                    bearer.getBearerToken(),
                    offer.getDriverId(),
                    offer.getDeliveryId()
            );
            deliveries.findByIdForUpdate(offer.getDeliveryId()).ifPresent(matching::offerNext);
        }
    }
}
