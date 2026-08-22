package com.khanh.fooddelivery.delivery_service.service.impl;

import com.khanh.fooddelivery.delivery_service.client.DriverServiceClient;
import com.khanh.fooddelivery.delivery_service.client.OrderServiceClient;
import com.khanh.fooddelivery.delivery_service.dto.response.DeliveryOfferResponse;
import com.khanh.fooddelivery.delivery_service.dto.response.CurrentDeliveryOfferResponse;
import com.khanh.fooddelivery.delivery_service.dto.response.DeliveryResponse;
import com.khanh.fooddelivery.delivery_service.exception.AppException;
import com.khanh.fooddelivery.delivery_service.exception.ErrorCode;
import com.khanh.fooddelivery.delivery_service.mapper.DeliveryMapper;
import com.khanh.fooddelivery.delivery_service.model.Delivery;
import com.khanh.fooddelivery.delivery_service.model.DeliveryOffer;
import com.khanh.fooddelivery.delivery_service.model.DeliveryOfferStatus;
import com.khanh.fooddelivery.delivery_service.model.DeliveryStatus;
import com.khanh.fooddelivery.delivery_service.repository.DeliveryOfferRepository;
import com.khanh.fooddelivery.delivery_service.repository.DeliveryRepository;
import com.khanh.fooddelivery.delivery_service.security.CurrentBearerTokenProvider;
import com.khanh.fooddelivery.delivery_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.delivery_service.service.DriverDispatchService;
import com.khanh.fooddelivery.delivery_service.service.DeliveryOfferService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.beans.factory.annotation.Value;
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
    private final DriverDispatchService dispatch;
    private final DeliveryMapper mapper;

    @Value("${app.internal-api.key:}")
    private String internalApiKey;

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
    @Transactional(readOnly = true)
    public Optional<CurrentDeliveryOfferResponse> currentOffer(Jwt jwt) {
        UUID driverId = users.getCurrentUserId(jwt);
        return offers.findCurrentByDriverId(
                        driverId,
                        DeliveryOfferStatus.PENDING,
                        Instant.now()
                ).stream()
                .findFirst()
                .flatMap(offer -> deliveries.findById(offer.getDeliveryId())
                        .filter(delivery -> delivery.getStatus() == DeliveryStatus.MATCHING)
                        .map(delivery -> mapper.toCurrentOffer(offer, delivery)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeliveryResponse> currentActiveDelivery(Jwt jwt) {
        UUID driverId = users.getCurrentUserId(jwt);
        return deliveries.findByDriverIdAndStatusInOrderByUpdatedAtDesc(
                        driverId,
                        List.of(DeliveryStatus.ASSIGNED, DeliveryStatus.PICKED_UP)
                ).stream()
                .findFirst()
                .map(mapper::toResponse);
    }

    @Override
    public DeliveryResponse accept(Jwt jwt, UUID deliveryId) {
        UUID driverId = users.getCurrentUserId(jwt);
        Delivery delivery = deliveries.findByIdForUpdate(deliveryId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.DELIVERY_CONFLICT,
                        "Delivery is no longer available"
                ));
        DeliveryOffer offer = offers.findByDeliveryIdAndDriverIdAndStatusForUpdate(
                deliveryId,
                driverId,
                DeliveryOfferStatus.PENDING
        ).orElseThrow(() -> new AppException(
                ErrorCode.DELIVERY_CONFLICT,
                "Delivery offer is no longer available"
        ));

        if (delivery.getStatus() != DeliveryStatus.MATCHING
                || !offer.getExpiresAt().isAfter(Instant.now())) {
            throw new AppException(
                    ErrorCode.DELIVERY_CONFLICT,
                    "Delivery offer is no longer active"
            );
        }

        drivers.acceptOffer(downstreamCredential(), driverId, deliveryId);
        offer.setStatus(DeliveryOfferStatus.ACCEPTED);
        offer.setRespondedAt(Instant.now());
        delivery.setDriverId(driverId);
        delivery.setStatus(DeliveryStatus.ASSIGNED);
        orders.assigned(downstreamCredential(), delivery.getOrderId());
        return mapper.toResponse(delivery);
    }

    @Override
    public void reject(Jwt jwt, UUID deliveryId) {
        UUID driverId = users.getCurrentUserId(jwt);
        DeliveryOffer offer = offers.findByDeliveryIdAndDriverIdAndStatusForUpdate(
                deliveryId,
                driverId,
                DeliveryOfferStatus.PENDING
        ).orElseThrow(() -> new AppException(
                ErrorCode.DELIVERY_CONFLICT,
                "Delivery offer is no longer available"
        ));
        offer.setStatus(DeliveryOfferStatus.REJECTED);
        offer.setRespondedAt(Instant.now());
        drivers.releaseOffer(downstreamCredential(), driverId, deliveryId);
        deliveries.findByIdForUpdate(deliveryId).ifPresent(delivery ->
                dispatch.schedule(delivery, Instant.now()));
    }

    @Override
    @Scheduled(fixedDelay = 30000)
    public void expireOffers() {
        for (DeliveryOffer offer : offers.findExpired(Instant.now())) {
            DeliveryOffer current = offers.findByIdForUpdate(offer.getId()).orElse(null);
            if (current == null
                    || current.getStatus() != DeliveryOfferStatus.PENDING
                    || current.getExpiresAt().isAfter(Instant.now())) {
                continue;
            }
            current.setStatus(DeliveryOfferStatus.EXPIRED);
            current.setRespondedAt(Instant.now());
            drivers.releaseOffer(
                    downstreamCredential(),
                    current.getDriverId(),
                    current.getDeliveryId()
            );
            deliveries.findByIdForUpdate(current.getDeliveryId()).ifPresent(delivery ->
                    dispatch.schedule(delivery, Instant.now()));
        }
    }

    private String downstreamCredential() {
        try {
            return bearer.getBearerToken();
        } catch (AppException exception) {
            if (internalApiKey == null || internalApiKey.isBlank()) {
                throw exception;
            }
            return "Internal " + internalApiKey;
        }
    }
}
