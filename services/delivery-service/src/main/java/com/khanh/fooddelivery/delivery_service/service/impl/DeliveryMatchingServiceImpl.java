package com.khanh.fooddelivery.delivery_service.service.impl;

import com.khanh.fooddelivery.delivery_service.client.DriverServiceClient;
import com.khanh.fooddelivery.delivery_service.client.OrderServiceClient;
import com.khanh.fooddelivery.delivery_service.client.RestaurantServiceClient;
import com.khanh.fooddelivery.delivery_service.client.TrackingServiceClient;
import com.khanh.fooddelivery.delivery_service.client.dto.response.NearestDriverResponse;
import com.khanh.fooddelivery.delivery_service.dto.request.DeliveryMatchingRequest;
import com.khanh.fooddelivery.delivery_service.dto.response.DeliveryResponse;
import com.khanh.fooddelivery.delivery_service.mapper.DeliveryMapper;
import com.khanh.fooddelivery.delivery_service.model.Delivery;
import com.khanh.fooddelivery.delivery_service.model.DeliveryOffer;
import com.khanh.fooddelivery.delivery_service.model.DeliveryOfferStatus;
import com.khanh.fooddelivery.delivery_service.model.DeliveryStatus;
import com.khanh.fooddelivery.delivery_service.repository.DeliveryOfferRepository;
import com.khanh.fooddelivery.delivery_service.repository.DeliveryRepository;
import com.khanh.fooddelivery.delivery_service.security.CurrentBearerTokenProvider;
import com.khanh.fooddelivery.delivery_service.service.DeliveryMatchingService;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryMatchingServiceImpl implements DeliveryMatchingService {

    private final DeliveryRepository deliveries;
    private final DeliveryOfferRepository offers;
    private final CurrentBearerTokenProvider bearer;
    private final OrderServiceClient orders;
    private final DriverServiceClient drivers;
    private final RestaurantServiceClient restaurants;
    private final TrackingServiceClient tracking;
    private final DeliveryMapper mapper;

    @Value("${delivery.offer.ttl:45s}")
    private Duration offerTtl;

    @Value("${delivery.dispatch.search-radii-meters:2000,4000,6000,8000}")
    private List<Double> searchRadii;

    @Value("${delivery.dispatch.candidates-per-radius:20}")
    private long candidatesPerRadius;

    @Override
    public DeliveryResponse startMatching(DeliveryMatchingRequest request) {
        Delivery delivery = deliveries.findByOrderId(request.orderId())
                .orElseGet(() -> create(request));
        offerNext(delivery);
        return mapper.toResponse(delivery);
    }

    @Override
    public void offerNext(Delivery delivery) {
        if (delivery.getStatus() != DeliveryStatus.MATCHING) {
            return;
        }
        if (delivery.getPickupLatitude() == null || delivery.getPickupLongitude() == null) {
            delivery.setStatus(DeliveryStatus.MATCH_FAILED);
            orders.matchingFailed(bearer.getBearerToken(), delivery.getOrderId());
            return;
        }

        Set<UUID> excluded = offers.findAll().stream()
                .filter(offer -> offer.getDeliveryId().equals(delivery.getId()))
                .filter(offer -> offer.getStatus() == DeliveryOfferStatus.REJECTED
                        || offer.getStatus() == DeliveryOfferStatus.EXPIRED)
                .map(DeliveryOffer::getDriverId)
                .collect(Collectors.toSet());
        Set<UUID> available = new HashSet<>(drivers.available(bearer.getBearerToken()));

        UUID driverId = searchRadii.stream()
                .flatMap(radius -> tracking.nearest(
                                bearer.getBearerToken(),
                                delivery.getPickupLatitude(),
                                delivery.getPickupLongitude(),
                                radius,
                                candidatesPerRadius
                        ).stream())
                .map(NearestDriverResponse::driverId)
                .filter(available::contains)
                .filter(id -> !excluded.contains(id))
                .findFirst()
                .orElse(null);

        if (driverId == null) {
            delivery.setStatus(DeliveryStatus.MATCH_FAILED);
            orders.matchingFailed(bearer.getBearerToken(), delivery.getOrderId());
            return;
        }

        drivers.reserveOffer(bearer.getBearerToken(), driverId, delivery.getId());
        DeliveryOffer offer = new DeliveryOffer();
        offer.setId(UUID.randomUUID());
        offer.setDeliveryId(delivery.getId());
        offer.setDriverId(driverId);
        offer.setStatus(DeliveryOfferStatus.PENDING);
        offer.setExpiresAt(Instant.now().plus(offerTtl));
        offers.save(offer);
    }

    private Delivery create(DeliveryMatchingRequest request) {
        var branch = restaurants.getOrderingContext(
                bearer.getBearerToken(),
                request.branchId()
        ).data();
        if (branch == null || branch.latitude() == null || branch.longitude() == null) {
            throw new IllegalStateException("Branch location is unavailable");
        }

        Delivery delivery = new Delivery();
        delivery.setId(UUID.randomUUID());
        delivery.setOrderId(request.orderId());
        delivery.setRestaurantId(request.restaurantId());
        delivery.setBranchId(request.branchId());
        delivery.setCustomerId(request.customerId());
        delivery.setRestaurantName(request.restaurantName());
        delivery.setBranchName(request.branchName());
        delivery.setCustomerAddress(request.customerAddress());
        delivery.setPickupLatitude(branch.latitude());
        delivery.setPickupLongitude(branch.longitude());
        delivery.setStatus(DeliveryStatus.MATCHING);
        return deliveries.save(delivery);
    }
}
