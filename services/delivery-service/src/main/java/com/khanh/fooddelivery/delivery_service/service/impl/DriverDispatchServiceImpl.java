package com.khanh.fooddelivery.delivery_service.service.impl;

import com.khanh.fooddelivery.delivery_service.client.DriverServiceClient;
import com.khanh.fooddelivery.delivery_service.client.OrderServiceClient;
import com.khanh.fooddelivery.delivery_service.client.TrackingServiceClient;
import com.khanh.fooddelivery.delivery_service.client.dto.response.NearestDriverResponse;
import com.khanh.fooddelivery.delivery_service.model.Delivery;
import com.khanh.fooddelivery.delivery_service.model.DeliveryOffer;
import com.khanh.fooddelivery.delivery_service.model.DeliveryOfferStatus;
import com.khanh.fooddelivery.delivery_service.model.DeliveryStatus;
import com.khanh.fooddelivery.delivery_service.repository.DeliveryOfferRepository;
import com.khanh.fooddelivery.delivery_service.repository.DeliveryRepository;
import com.khanh.fooddelivery.delivery_service.security.CurrentBearerTokenProvider;
import com.khanh.fooddelivery.delivery_service.exception.AppException;
import com.khanh.fooddelivery.delivery_service.service.DriverDispatchService;
import com.khanh.fooddelivery.delivery_service.outbox.DeliveryOutboxService;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverDispatchServiceImpl implements DriverDispatchService {

    private final DeliveryRepository deliveries;
    private final DeliveryOfferRepository offers;
    private final CurrentBearerTokenProvider bearer;
    private final OrderServiceClient orders;
    private final DriverServiceClient drivers;
    private final TrackingServiceClient tracking;
    private final DeliveryOutboxService outbox;

    @Value("${delivery.offer.ttl:45s}")
    private Duration offerTtl;

    @Value("${delivery.dispatch.search-radii-meters:2000,4000,6000,8000}")
    private List<Double> searchRadii;

    @Value("${delivery.dispatch.candidates-per-radius:20}")
    private long candidatesPerRadius;

    @Value("${delivery.dispatch.retry-delay:15s}")
    private Duration retryDelay;

    @Value("${delivery.dispatch.max-duration:10m}")
    private Duration dispatchMaxDuration;

    @Value("${app.internal-api.key:}")
    private String internalApiKey;

    @Override
    @Transactional
    public void dispatch(UUID deliveryId) {
        Delivery delivery = deliveries.findByIdForUpdate(deliveryId).orElse(null);
        if (delivery == null || delivery.getStatus() != DeliveryStatus.MATCHING) {
            return;
        }
        initializeSchedule(delivery);
        Instant now = Instant.now();
        if (delivery.getNextDispatchAt() != null && delivery.getNextDispatchAt().isAfter(now)) {
            return;
        }
        if (offers.existsByDeliveryIdAndStatus(deliveryId, DeliveryOfferStatus.PENDING)) {
            return;
        }

        if (deadlineReached(delivery, now)) {
            finalizeFailure(delivery, now);
            return;
        }

        try {
            UUID driverId = findCandidate(delivery);
            delivery.setDispatchAttemptCount(delivery.getDispatchAttemptCount() + 1);
            if (driverId == null) {
                scheduleOrFinalize(delivery, now);
                log.debug("No eligible driver for deliveryId={} attempt={} nextDispatchAt={}",
                        delivery.getId(), delivery.getDispatchAttemptCount(), delivery.getNextDispatchAt());
                return;
            }

            String credential = downstreamCredential();
            drivers.reserveOffer(credential, driverId, delivery.getId());
            DeliveryOffer offer = new DeliveryOffer();
            offer.setId(UUID.randomUUID());
            offer.setDeliveryId(delivery.getId());
            offer.setDriverId(driverId);
            offer.setStatus(DeliveryOfferStatus.PENDING);
            offer.setExpiresAt(now.plus(offerTtl));
            boolean offerPersisted = false;
            try {
                offers.save(offer);
                offerPersisted = true;
            } catch (RuntimeException exception) {
                // Reservation is an external side effect. Release it when the
                // local offer cannot be persisted so a retry cannot strand the
                // driver as busy.
                try {
                    drivers.releaseOffer(credential, driverId, delivery.getId());
                } catch (RuntimeException releaseException) {
                    log.error("Could not release reservation after offer persistence failure deliveryId={} driverId={}",
                            delivery.getId(), driverId, releaseException);
                }
                throw exception;
            }
            try {
                outbox.publishDeliveryOfferCreated(offer);
            } catch (RuntimeException exception) {
                // Offer and outbox must commit together. Release the external
                // reservation before rolling the local transaction back.
                try {
                    drivers.releaseOffer(credential, driverId, delivery.getId());
                } catch (RuntimeException releaseException) {
                    log.error("Could not release reservation after outbox persistence failure deliveryId={} driverId={}",
                            delivery.getId(), driverId, releaseException);
                }
                if (offerPersisted && TransactionSynchronizationManager.isActualTransactionActive()) {
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                }
                log.warn("Delivery offer outbox persistence failed deliveryId={} driverId={} reasonType={}",
                        delivery.getId(), driverId, exception.getClass().getSimpleName());
                return;
            }
            delivery.setNextDispatchAt(null);
        } catch (RuntimeException exception) {
            scheduleOrFinalize(delivery, now);
            log.warn("Driver dispatch attempt failed deliveryId={} attempt={} reasonType={}",
                    delivery.getId(), delivery.getDispatchAttemptCount(), exception.getClass().getSimpleName());
        }
    }

    @Override
    @Transactional
    public void schedule(Delivery delivery, Instant nextDispatchAt) {
        if (delivery == null || delivery.getStatus() != DeliveryStatus.MATCHING) {
            return;
        }
        initializeSchedule(delivery);
        Instant requested = nextDispatchAt == null ? Instant.now() : nextDispatchAt;
        delivery.setNextDispatchAt(requested);
        deliveries.save(delivery);
    }

    private UUID findCandidate(Delivery delivery) {
        String credential = downstreamCredential();
        List<UUID> available = drivers.available(credential);
        if (available == null || available.isEmpty()) {
            return null;
        }
        Set<UUID> availableIds = new HashSet<>(available);
        List<DeliveryOffer> history = offers.findByDeliveryIdOrderByOfferedAtAsc(delivery.getId());
        if (history == null) {
            history = List.of();
        }
        Set<UUID> excluded = history.stream()
                .filter(offer -> offer.getStatus() == DeliveryOfferStatus.REJECTED
                        || offer.getStatus() == DeliveryOfferStatus.EXPIRED)
                .map(DeliveryOffer::getDriverId)
                .collect(Collectors.toSet());

        for (Double radius : searchRadii) {
            List<NearestDriverResponse> nearby = tracking.nearest(
                    credential,
                    delivery.getPickupLatitude(),
                    delivery.getPickupLongitude(),
                    radius,
                    candidatesPerRadius
            );
            if (nearby == null) {
                continue;
            }
            UUID candidate = nearby.stream()
                    .map(NearestDriverResponse::driverId)
                    .filter(availableIds::contains)
                    .filter(id -> !excluded.contains(id))
                    .findFirst()
                    .orElse(null);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private void scheduleOrFinalize(Delivery delivery, Instant now) {
        if (deadlineReached(delivery, now)) {
            finalizeFailure(delivery, now);
            return;
        }
        delivery.setNextDispatchAt(now.plus(retryDelay));
    }

    private void finalizeFailure(Delivery delivery, Instant now) {
        try {
            orders.matchingFailed(downstreamCredential(), delivery.getOrderId());
            delivery.setStatus(DeliveryStatus.MATCH_FAILED);
            delivery.setNextDispatchAt(null);
        } catch (RuntimeException exception) {
            // Keep the delivery retryable until the terminal order callback succeeds.
            delivery.setNextDispatchAt(now.plus(retryDelay));
            log.error("Terminal dispatch failure callback failed deliveryId={} orderId={}",
                    delivery.getId(), delivery.getOrderId(), exception);
        }
    }

    private boolean deadlineReached(Delivery delivery, Instant now) {
        return delivery.getDispatchDeadlineAt() != null
                && !now.isBefore(delivery.getDispatchDeadlineAt());
    }

    private void initializeSchedule(Delivery delivery) {
        Instant started = delivery.getMatchingStartedAt();
        if (started == null) {
            started = delivery.getCreatedAt() == null ? Instant.now() : delivery.getCreatedAt();
            delivery.setMatchingStartedAt(started);
        }
        if (delivery.getDispatchDeadlineAt() == null) {
            delivery.setDispatchDeadlineAt(started.plus(dispatchMaxDuration));
        }
        if (delivery.getDispatchAttemptCount() < 0) {
            delivery.setDispatchAttemptCount(0);
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
