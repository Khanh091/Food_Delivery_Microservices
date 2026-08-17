package com.khanh.fooddelivery.delivery_service.service.impl;

import com.khanh.fooddelivery.delivery_service.client.RemoteApiResponse;
import com.khanh.fooddelivery.delivery_service.client.RestaurantServiceClient;
import com.khanh.fooddelivery.delivery_service.client.UserServiceClient;
import com.khanh.fooddelivery.delivery_service.config.DeliveryQuoteProperties;
import com.khanh.fooddelivery.delivery_service.dto.request.CreateDeliveryQuoteRequest;
import com.khanh.fooddelivery.delivery_service.dto.request.DeliveryTargetRequest;
import com.khanh.fooddelivery.delivery_service.dto.response.DeliveryQuoteResponse;
import com.khanh.fooddelivery.delivery_service.exception.AppException;
import com.khanh.fooddelivery.delivery_service.exception.ErrorCode;
import com.khanh.fooddelivery.delivery_service.model.DeliveryQuote;
import com.khanh.fooddelivery.delivery_service.model.DeliveryTargetType;
import com.khanh.fooddelivery.delivery_service.model.CheckoutTemporaryLocation;
import com.khanh.fooddelivery.delivery_service.repository.CheckoutTemporaryLocationRepository;
import com.khanh.fooddelivery.delivery_service.repository.DeliveryQuoteRepository;
import com.khanh.fooddelivery.delivery_service.security.CurrentBearerTokenProvider;
import com.khanh.fooddelivery.delivery_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.delivery_service.service.DeliveryQuoteService;
import com.khanh.fooddelivery.delivery_service.service.RoutingProvider;
import feign.FeignException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeliveryQuoteServiceImpl implements DeliveryQuoteService {
    private final RestaurantServiceClient restaurantServiceClient;
    private final UserServiceClient userServiceClient;
    private final CurrentUserProvider currentUserProvider;
    private final CurrentBearerTokenProvider bearerTokenProvider;
    private final RoutingProvider routingProvider;
    private final DeliveryQuoteRepository quoteRepository;
    private final CheckoutTemporaryLocationRepository checkoutTemporaryLocationRepository;
    private final DeliveryQuoteProperties properties;

    @Override
    public DeliveryQuoteResponse createQuote(Jwt jwt, CreateDeliveryQuoteRequest request) {
        UUID ownerUserId = currentUserProvider.getCurrentUserId(jwt);
        String bearer = bearerTokenProvider.getBearerToken();
        RestaurantServiceClient.RestaurantBranchOrderingContextResponse branch = requireBranch(bearer, request.branchId());
        TargetLocation target = resolveTarget(ownerUserId, bearer, request.branchId(), request.target());
        validateCoordinates(branch.latitude(), branch.longitude());
        validateCoordinates(target.latitude(), target.longitude());
        RoutingProvider.Route route = routingProvider.calculateRoute(
                branch.latitude(), branch.longitude(), target.latitude(), target.longitude());
        if (route.distanceMeters() > properties.getMaximumServiceDistanceMeters()) {
            throw new AppException(ErrorCode.DELIVERY_NOT_SERVICEABLE);
        }
        BigDecimal fee = calculateFee(route.distanceMeters());
        Instant calculatedAt = Instant.now();
        Instant expiresAt = calculatedAt.plus(properties.getTtl());
        DeliveryQuote quote = new DeliveryQuote(
                UUID.randomUUID(), ownerUserId, request.branchId(), target.type(), target.addressId(), target.temporaryLocationId(),
                properties.getCurrency(), fee,
                route.distanceMeters(), durationMinutes(route.durationSeconds()), properties.getPricingPolicyVersion(),
                calculatedAt, expiresAt);
        quoteRepository.save(quote, properties.getTtl());
        return toResponse(quote);
    }

    BigDecimal calculateFee(long distanceMeters) {
        long chargeableMeters = Math.max(0, distanceMeters - properties.getIncludedDistanceMeters());
        BigDecimal extra = properties.getFeePerKm()
                .multiply(BigDecimal.valueOf(chargeableMeters))
                .divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);
        return properties.getBaseFee().add(extra).setScale(0, RoundingMode.HALF_UP);
    }

    private RestaurantServiceClient.RestaurantBranchOrderingContextResponse requireBranch(String bearer, UUID branchId) {
        try {
            RemoteApiResponse<RestaurantServiceClient.RestaurantBranchOrderingContextResponse> response =
                    restaurantServiceClient.getOrderingContext(bearer, branchId);
            if (response == null || !response.success() || response.data() == null) {
                throw new AppException(ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE);
            }
            RestaurantServiceClient.RestaurantBranchOrderingContextResponse branch = response.data();
            if (!branch.restaurantActive() || !branch.branchActive() || !branch.acceptingOrders()) {
                throw new AppException(ErrorCode.DELIVERY_NOT_SERVICEABLE);
            }
            return branch;
        } catch (FeignException.NotFound exception) {
            throw new AppException(ErrorCode.DELIVERY_NOT_SERVICEABLE);
        } catch (FeignException exception) {
            throw new AppException(ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE);
        }
    }

    private UserServiceClient.InternalUserAddressResponse requireAddress(String bearer, UUID addressId) {
        try {
            RemoteApiResponse<UserServiceClient.InternalUserAddressResponse> response = userServiceClient.getOwnedAddress(bearer, addressId);
            if (response == null || !response.success() || response.data() == null) {
                throw new AppException(ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE);
            }
            return response.data();
        } catch (FeignException.NotFound exception) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        } catch (FeignException exception) {
            throw new AppException(ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE);
        }
    }

    private TargetLocation resolveTarget(UUID ownerUserId, String bearer, UUID branchId, DeliveryTargetRequest target) {
        if (target.type() == DeliveryTargetType.SAVED_ADDRESS) {
            UserServiceClient.InternalUserAddressResponse address = requireAddress(bearer, target.addressId());
            if (address.latitude() == null || address.longitude() == null) {
                throw new AppException(ErrorCode.ADDRESS_COORDINATES_MISSING);
            }
            return new TargetLocation(DeliveryTargetType.SAVED_ADDRESS, address.id(), null, address.latitude(), address.longitude());
        }
        CheckoutTemporaryLocation location = checkoutTemporaryLocationRepository.findCurrent(ownerUserId, branchId)
                .filter(current -> current.id().equals(target.temporaryLocationId()))
                .orElseThrow(() -> new AppException(ErrorCode.CHECKOUT_LOCATION_NOT_FOUND));
        return new TargetLocation(DeliveryTargetType.TEMPORARY_LOCATION, null, location.id(), location.latitude(), location.longitude());
    }

    private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null || latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0 || longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new AppException(ErrorCode.ROUTE_NOT_FOUND);
        }
    }

    private long durationMinutes(long seconds) { return Math.max(1, (seconds + 59) / 60); }

    private DeliveryQuoteResponse toResponse(DeliveryQuote quote) {
        return new DeliveryQuoteResponse(quote.quoteId(), true, quote.currency(), quote.deliveryFee(), quote.distanceMeters(),
                quote.estimatedDurationMinutes(), quote.pricingPolicyVersion(), quote.calculatedAt(), quote.expiresAt());
    }

    private record TargetLocation(DeliveryTargetType type, UUID addressId, UUID temporaryLocationId,
                                  BigDecimal latitude, BigDecimal longitude) {}
}
