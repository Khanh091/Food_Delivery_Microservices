package com.khanh.fooddelivery.delivery_service.service.impl;

import com.khanh.fooddelivery.delivery_service.client.DriverServiceClient;
import com.khanh.fooddelivery.delivery_service.client.OrderServiceClient;
import com.khanh.fooddelivery.delivery_service.client.RestaurantServiceClient;
import com.khanh.fooddelivery.delivery_service.client.TrackingServiceClient;
import com.khanh.fooddelivery.delivery_service.client.PaymentServiceClient;
import com.khanh.fooddelivery.delivery_service.client.dto.response.NearestDriverResponse;
import com.khanh.fooddelivery.delivery_service.dto.request.DeliveryMatchingRequest;
import com.khanh.fooddelivery.delivery_service.dto.response.DeliveryResponse;
import com.khanh.fooddelivery.delivery_service.exception.AppException;
import com.khanh.fooddelivery.delivery_service.mapper.DeliveryMapper;
import com.khanh.fooddelivery.delivery_service.model.Delivery;
import com.khanh.fooddelivery.delivery_service.model.DeliveryOffer;
import com.khanh.fooddelivery.delivery_service.model.DeliveryOfferStatus;
import com.khanh.fooddelivery.delivery_service.model.DeliveryStatus;
import com.khanh.fooddelivery.delivery_service.repository.DeliveryOfferRepository;
import com.khanh.fooddelivery.delivery_service.repository.DeliveryRepository;
import com.khanh.fooddelivery.delivery_service.security.CurrentBearerTokenProvider;
import com.khanh.fooddelivery.delivery_service.service.DeliveryMatchingService;
import com.khanh.fooddelivery.delivery_service.service.event.DeliveryOfferCreatedEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
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
    private final ApplicationEventPublisher events;
    private final PaymentServiceClient payment;

    @Value("${delivery.offer.ttl:45s}")
    private Duration offerTtl;

    @Value("${delivery.dispatch.search-radii-meters:2000,4000,6000,8000}")
    private List<Double> searchRadii;

    @Value("${delivery.dispatch.candidates-per-radius:20}")
    private long candidatesPerRadius;

    @Value("${app.internal-api.key:}")
    private String internalApiKey;

    @Autowired
    public DeliveryMatchingServiceImpl(
            DeliveryRepository deliveries,
            DeliveryOfferRepository offers,
            CurrentBearerTokenProvider bearer,
            OrderServiceClient orders,
            DriverServiceClient drivers,
            RestaurantServiceClient restaurants,
            TrackingServiceClient tracking,
            DeliveryMapper mapper,
            ApplicationEventPublisher events,
            PaymentServiceClient payment
    ) {
        this.deliveries = deliveries;
        this.offers = offers;
        this.bearer = bearer;
        this.orders = orders;
        this.drivers = drivers;
        this.restaurants = restaurants;
        this.tracking = tracking;
        this.mapper = mapper;
        this.events = events;
        this.payment = payment;
    }

    /**
     * Retained for focused unit tests that exercise matching without payment facts.
     */
    public DeliveryMatchingServiceImpl(
            DeliveryRepository deliveries,
            DeliveryOfferRepository offers,
            CurrentBearerTokenProvider bearer,
            OrderServiceClient orders,
            DriverServiceClient drivers,
            RestaurantServiceClient restaurants,
            TrackingServiceClient tracking,
            DeliveryMapper mapper,
            ApplicationEventPublisher events
    ) {
        this(deliveries, offers, bearer, orders, drivers, restaurants, tracking, mapper, events, null);
    }

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
            orders.matchingFailed(downstreamCredential(), delivery.getOrderId());
            return;
        }

        if (offers.existsByDeliveryIdAndStatus(delivery.getId(), DeliveryOfferStatus.PENDING)) {
            return;
        }

        Set<UUID> excluded = offers.findAll().stream()
                .filter(offer -> offer.getDeliveryId().equals(delivery.getId()))
                .filter(offer -> offer.getStatus() == DeliveryOfferStatus.REJECTED
                        || offer.getStatus() == DeliveryOfferStatus.EXPIRED)
                .map(DeliveryOffer::getDriverId)
                .collect(Collectors.toSet());
        String credential = downstreamCredential();
        Set<UUID> available = new HashSet<>(drivers.available(credential));

        UUID driverId = searchRadii.stream()
                .flatMap(radius -> tracking.nearest(
                                credential,
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
            orders.matchingFailed(credential, delivery.getOrderId());
            return;
        }

        drivers.reserveOffer(credential, driverId, delivery.getId());
        DeliveryOffer offer = new DeliveryOffer();
        offer.setId(UUID.randomUUID());
        offer.setDeliveryId(delivery.getId());
        offer.setDriverId(driverId);
        offer.setStatus(DeliveryOfferStatus.PENDING);
        offer.setExpiresAt(Instant.now().plus(offerTtl));
        offers.save(offer);
        events.publishEvent(new DeliveryOfferCreatedEvent(
                driverId,
                offer.getId(),
                delivery.getId()
        ));
    }

    private Delivery create(DeliveryMatchingRequest request) {
        String internalCredential = internalCredential();
        var branch = restaurants.getOrderingContext(
                null,
                internalCredential,
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
        // Older orders may not have the new formatted snapshot yet. Keep the
        // delivery row valid while preserving the label as a last-resort
        // historical fallback; new orders always provide the full address.
        delivery.setCustomerAddress(firstNonBlank(
                request.customerAddress(), request.customerAddressLabel()));
        delivery.setCustomerAddressLabel(request.customerAddressLabel());
        delivery.setCustomerLatitude(request.customerLatitude());
        delivery.setCustomerLongitude(request.customerLongitude());
        delivery.setPickupLatitude(branch.latitude());
        delivery.setPickupLongitude(branch.longitude());
        delivery.setPickupAddress(formatAddress(
                branch.addressLine(), branch.ward(), branch.district(), branch.city()));
        try {
            if (payment == null) {
                delivery.setStatus(DeliveryStatus.MATCHING);
                return deliveries.save(delivery);
            }
            var factsResponse = payment.facts(internalApiKey, request.orderId());
            var facts = factsResponse == null || !factsResponse.success() ? null : factsResponse.data();
            if (facts == null) {
                throw new AppException(com.khanh.fooddelivery.delivery_service.exception.ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE,
                        "Financial facts are unavailable for this order");
            }
            delivery.setPaymentMethod(facts.paymentMethod());
            delivery.setRequiredRestaurantAdvance(facts.requiredRestaurantAdvance());
            delivery.setCustomerCashToCollect(facts.customerCashToCollect());
            delivery.setDriverGrossEarning(facts.driverGrossEarning());
            delivery.setRestaurantCommissionAmount(facts.restaurantCommissionAmount());
            delivery.setDriverCommissionAmount(facts.driverCommissionAmount());
            delivery.setDriverNetEarning(facts.driverNetEarning());
            delivery.setRestaurantNetAmount(facts.restaurantNetAmount());
            delivery.setPlatformRevenueAmount(facts.platformRevenueAmount());
            delivery.setRestaurantAdvanceConfirmed(facts.restaurantAdvanceConfirmed());
            delivery.setCustomerCashCollected(facts.customerCashCollected());
        } catch (feign.FeignException exception) {
            throw new AppException(com.khanh.fooddelivery.delivery_service.exception.ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE,
                    "Financial service is temporarily unavailable");
        }
        delivery.setStatus(DeliveryStatus.MATCHING);
        return deliveries.save(delivery);
    }

    private String formatAddress(String addressLine, String ward, String district, String city) {
        return java.util.stream.Stream.of(addressLine, ward, district, city)
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        return fallback == null || fallback.isBlank() ? null : fallback.trim();
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

    private String internalCredential() {
        if (internalApiKey == null || internalApiKey.isBlank()) {
            throw new AppException(
                    com.khanh.fooddelivery.delivery_service.exception.ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE,
                    "Internal service credential is not configured");
        }
        return internalApiKey;
    }
}
