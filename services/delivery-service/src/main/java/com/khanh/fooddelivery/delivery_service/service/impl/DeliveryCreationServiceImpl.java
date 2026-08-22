package com.khanh.fooddelivery.delivery_service.service.impl;

import com.khanh.fooddelivery.delivery_service.client.PaymentServiceClient;
import com.khanh.fooddelivery.delivery_service.client.RestaurantServiceClient;
import com.khanh.fooddelivery.delivery_service.client.dto.response.RestaurantBranchOrderingContextResponse;
import com.khanh.fooddelivery.delivery_service.event.OrderConfirmedEvent;
import com.khanh.fooddelivery.delivery_service.exception.AppException;
import com.khanh.fooddelivery.delivery_service.exception.ErrorCode;
import com.khanh.fooddelivery.delivery_service.model.Delivery;
import com.khanh.fooddelivery.delivery_service.model.DeliveryStatus;
import com.khanh.fooddelivery.delivery_service.repository.DeliveryRepository;
import com.khanh.fooddelivery.delivery_service.service.DeliveryCreationService;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeliveryCreationServiceImpl implements DeliveryCreationService {

    private final DeliveryRepository deliveries;
    private final RestaurantServiceClient restaurants;
    private final PaymentServiceClient payment;

    @Value("${app.internal-api.key:}")
    private String internalApiKey;

    @Value("${delivery.dispatch.max-duration:10m}")
    private Duration dispatchMaxDuration;

    @Override
    @Transactional
    public Delivery ensureMatching(OrderConfirmedEvent.Payload request) {
        validate(request);
        Delivery existing = deliveries.findByOrderIdForUpdate(request.orderId()).orElse(null);
        if (existing != null) {
            initializeLegacySchedule(existing);
            return existing;
        }

        RestaurantBranchOrderingContextResponse branch = orderingContext(request.branchId());
        if (branch.latitude() == null || branch.longitude() == null) {
            throw new AppException(
                    ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE,
                    "Branch location is unavailable"
            );
        }

        Delivery delivery = new Delivery();
        delivery.setId(UUID.randomUUID());
        delivery.setOrderId(request.orderId());
        delivery.setRestaurantId(request.restaurantId());
        delivery.setBranchId(request.branchId());
        delivery.setCustomerId(request.customerId());
        delivery.setRestaurantName(request.restaurantName());
        delivery.setBranchName(request.branchName());
        delivery.setCustomerAddress(firstNonBlank(
                request.customerAddress(),
                request.customerAddressLabel()
        ));
        delivery.setCustomerAddressLabel(request.customerAddressLabel());
        delivery.setCustomerLatitude(request.customerLatitude());
        delivery.setCustomerLongitude(request.customerLongitude());
        delivery.setPickupLatitude(branch.latitude());
        delivery.setPickupLongitude(branch.longitude());
        delivery.setPickupAddress(formatAddress(
                branch.addressLine(), branch.ward(), branch.district(), branch.city()));
        applyFinancialFacts(delivery, request.orderId());

        Instant now = Instant.now();
        delivery.setStatus(DeliveryStatus.MATCHING);
        delivery.setMatchingStartedAt(now);
        delivery.setNextDispatchAt(now);
        delivery.setDispatchDeadlineAt(now.plus(dispatchMaxDuration));
        delivery.setDispatchAttemptCount(0);
        return deliveries.save(delivery);
    }

    private RestaurantBranchOrderingContextResponse orderingContext(UUID branchId) {
        if (internalApiKey == null || internalApiKey.isBlank()) {
            throw new AppException(
                    ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE,
                    "Internal service credential is not configured"
            );
        }
        try {
            var response = restaurants.getOrderingContext(
                    null,
                    internalApiKey,
                    branchId
            );
            if (response == null || !response.success() || response.data() == null) {
                throw new AppException(
                        ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE,
                        "Restaurant ordering context is unavailable"
                );
            }
            return response.data();
        } catch (feign.FeignException exception) {
            throw new AppException(
                    ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE,
                    "Restaurant service is temporarily unavailable"
            );
        }
    }

    private void applyFinancialFacts(Delivery delivery, UUID orderId) {
        if (payment == null) {
            return;
        }
        if (internalApiKey == null || internalApiKey.isBlank()) {
            throw new AppException(
                    ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE,
                    "Internal service credential is not configured"
            );
        }
        try {
            var response = payment.facts(internalApiKey, orderId);
            var facts = response == null || !response.success() ? null : response.data();
            if (facts == null) {
                throw new AppException(
                        ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE,
                        "Financial facts are unavailable for this order"
                );
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
            throw new AppException(
                    ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE,
                    "Financial service is temporarily unavailable"
            );
        }
    }

    private void initializeLegacySchedule(Delivery delivery) {
        if (delivery.getStatus() != DeliveryStatus.MATCHING) {
            return;
        }
        Instant started = delivery.getMatchingStartedAt();
        if (started == null) {
            started = delivery.getCreatedAt() == null ? Instant.now() : delivery.getCreatedAt();
            delivery.setMatchingStartedAt(started);
        }
        if (delivery.getDispatchDeadlineAt() == null) {
            delivery.setDispatchDeadlineAt(started.plus(dispatchMaxDuration));
        }
        if (delivery.getNextDispatchAt() == null) {
            delivery.setNextDispatchAt(Instant.now());
        }
    }

    private void validate(OrderConfirmedEvent.Payload request) {
        if (request == null || request.orderId() == null || request.restaurantId() == null
                || request.branchId() == null || request.customerId() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Delivery order references are required");
        }
    }

    private String formatAddress(String addressLine, String ward, String district, String city) {
        return java.util.stream.Stream.of(addressLine, ward, district, city)
                .filter(Objects::nonNull)
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
}
