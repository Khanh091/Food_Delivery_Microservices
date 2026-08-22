package com.khanh.fooddelivery.order_service.service.impl;

import com.khanh.fooddelivery.order_service.client.CartServiceClient;
import com.khanh.fooddelivery.order_service.client.CatalogServiceClient;
import com.khanh.fooddelivery.order_service.client.DeliveryServiceClient;
import com.khanh.fooddelivery.order_service.common.response.ApiResponse;
import com.khanh.fooddelivery.order_service.client.dto.request.CheckoutItemRequest;
import com.khanh.fooddelivery.order_service.client.dto.request.CheckoutItemsValidationRequest;
import com.khanh.fooddelivery.order_service.client.dto.request.DeliveryTargetRequest;
import com.khanh.fooddelivery.order_service.client.dto.request.DeliveryQuoteRequest;
import com.khanh.fooddelivery.order_service.client.dto.response.*;
import com.khanh.fooddelivery.order_service.client.RestaurantServiceClient;
import com.khanh.fooddelivery.order_service.client.UserServiceClient;
import com.khanh.fooddelivery.order_service.dto.request.CheckoutPreviewRequest;
import com.khanh.fooddelivery.order_service.dto.request.CheckoutDeliveryTargetRequest;
import com.khanh.fooddelivery.order_service.dto.response.CheckoutPreviewResponse;
import com.khanh.fooddelivery.order_service.dto.response.DeliveryQuoteStatus;
import com.khanh.fooddelivery.order_service.exception.AppException;
import com.khanh.fooddelivery.order_service.exception.ErrorCode;
import com.khanh.fooddelivery.order_service.security.CurrentBearerTokenProvider;
import com.khanh.fooddelivery.order_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.order_service.service.CheckoutPreviewFingerprint;
import com.khanh.fooddelivery.order_service.service.CheckoutPreviewService;
import feign.FeignException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CheckoutPreviewServiceImpl implements CheckoutPreviewService {
    private final CartServiceClient cartServiceClient;
    private final UserServiceClient userServiceClient;
    private final RestaurantServiceClient restaurantServiceClient;
    private final CatalogServiceClient catalogServiceClient;
    private final DeliveryServiceClient deliveryServiceClient;
    private final CurrentUserProvider currentUserProvider;
    private final CurrentBearerTokenProvider bearerTokenProvider;
    private final CheckoutPreviewFingerprint fingerprint;

    @Override
    public CheckoutPreviewResponse preview(Jwt jwt, CheckoutPreviewRequest request) {
        UUID ownerUserId = currentUserProvider.getCurrentUserId(jwt);
        String bearer = bearerTokenProvider.getBearerToken();
        InternalCartSnapshotResponse cart = requireCart(bearer, request.branchId());
        validateCart(ownerUserId, cart, request.branchId(), request.cartVersion());
        CheckoutPreviewResponse.CheckoutAddressSnapshot address = requireDeliveryTarget(bearer, request.branchId(), request.target());
        RestaurantBranchCartAvailabilityResponse branch =
                requireRestaurant(bearer, cart.restaurantId(), cart.branchId());
        List<ValidatedCheckoutItemResponse> validated =
                requireCatalogValidation(bearer, cart);
        Map<UUID, ValidatedCheckoutItemResponse> validatedByCartItem = indexValidated(cart, validated);
        String currency = resolveCurrency(validated);
        List<CheckoutPreviewResponse.CheckoutItemResponse> items = cart.items().stream()
                .map(item -> toPreviewItem(item, validatedByCartItem.get(item.cartItemId())))
                .toList();
        BigDecimal subtotal = items.stream()
                .map(CheckoutPreviewResponse.CheckoutItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<CheckoutPreviewResponse.PriceChangeResponse> priceChanges = cart.items().stream()
                .map(item -> priceChange(item, validatedByCartItem.get(item.cartItemId())))
                .filter(change -> change != null)
                .toList();
        CheckoutPreviewResponse.CheckoutRestaurantSnapshot restaurant =
                new CheckoutPreviewResponse.CheckoutRestaurantSnapshot(branch.restaurantId(), branch.restaurantName());
        CheckoutPreviewResponse.CheckoutBranchSnapshot branchSnapshot =
                new CheckoutPreviewResponse.CheckoutBranchSnapshot(branch.branchId(), branch.branchName());
        BigDecimal discountAmount = BigDecimal.ZERO;
        DeliveryResolution delivery = resolveDelivery(bearer, cart.branchId(), address, currency);
        BigDecimal totalAmount = delivery.quote() == null ? null : subtotal.subtract(discountAmount).add(delivery.quote().deliveryFee());
        String previewFingerprint = fingerprint.of(
                ownerUserId, cart.version(), address, restaurant, branchSnapshot, items, currency, subtotal,
                discountAmount, delivery.status(), delivery.quoteId(), delivery.deliveryFee(),
                delivery.expiresAt(), delivery.pricingPolicyVersion());
        return new CheckoutPreviewResponse(
                cart.version(), address, restaurant, branchSnapshot, items, currency, subtotal, discountAmount,
                delivery.status(), delivery.quoteId(), delivery.expiresAt(), delivery.pricingPolicyVersion(),
                delivery.deliveryFee(), totalAmount, priceChanges, previewFingerprint, Instant.now(),
                delivery.status() == DeliveryQuoteStatus.AVAILABLE);
    }

    private InternalCartSnapshotResponse requireCart(String bearer, UUID branchId) {
        try {
            ApiResponse<InternalCartSnapshotResponse> response =
                    cartServiceClient.getCurrentSnapshot(bearer, branchId);
            if (response == null || !response.success() || response.data() == null) {
                throw new AppException(ErrorCode.CART_SERVICE_UNAVAILABLE);
            }
            return response.data();
        } catch (FeignException exception) {
            throw new AppException(ErrorCode.CART_SERVICE_UNAVAILABLE);
        }
    }

    private void validateCart(
            UUID ownerUserId, InternalCartSnapshotResponse cart, UUID expectedBranchId, long expectedVersion) {
        if (!ownerUserId.equals(cart.ownerUserId())) throw new AppException(ErrorCode.ACCESS_DENIED);
        if (cart.items() == null || cart.items().isEmpty()) throw new AppException(ErrorCode.CART_EMPTY);
        if (!expectedBranchId.equals(cart.branchId())) throw new AppException(ErrorCode.CART_SERVICE_UNAVAILABLE);
        if (cart.version() != expectedVersion) throw new AppException(ErrorCode.CART_VERSION_CONFLICT);
        if (cart.restaurantId() == null || cart.branchId() == null) throw new AppException(ErrorCode.CART_EMPTY);
    }

    private CheckoutPreviewResponse.CheckoutAddressSnapshot requireDeliveryTarget(
            String bearer, UUID branchId, CheckoutDeliveryTargetRequest target) {
        if ("TEMPORARY_LOCATION".equals(target.type())) return requireTemporaryLocation(bearer, branchId, target.temporaryLocationId());
        return requireAddress(bearer, target.addressId());
    }

    private CheckoutPreviewResponse.CheckoutAddressSnapshot requireAddress(String bearer, UUID addressId) {
        try {
            ApiResponse<InternalUserAddressResponse> response =
                    userServiceClient.getOwnedAddress(bearer, addressId);
            if (response == null || !response.success() || response.data() == null) {
                throw new AppException(ErrorCode.USER_SERVICE_UNAVAILABLE);
            }
            InternalUserAddressResponse address = response.data();
            return new CheckoutPreviewResponse.CheckoutAddressSnapshot(
                    "SAVED_ADDRESS", address.id(), null, address.labelType(), address.customLabel(), address.displayLabel(),
                    address.recipientName(), address.recipientPhone(), address.addressLine(), address.ward(),
                    address.district(), address.city(), address.latitude(), address.longitude(), address.buildingName(),
                    address.floor(), address.entrance(), address.deliveryNote(), address.version());
        } catch (FeignException.NotFound exception) {
            throw new AppException(ErrorCode.ADDRESS_NOT_FOUND);
        } catch (FeignException exception) {
            throw new AppException(ErrorCode.USER_SERVICE_UNAVAILABLE);
        }
    }

    private CheckoutPreviewResponse.CheckoutAddressSnapshot requireTemporaryLocation(
            String bearer, UUID branchId, UUID temporaryLocationId) {
        try {
            ApiResponse<CheckoutTemporaryLocationResponse> response =
                    deliveryServiceClient.getCurrentCheckoutLocation(bearer, branchId);
            if (response == null || !response.success() || response.data() == null
                    || !temporaryLocationId.equals(response.data().id())) {
                throw new AppException(ErrorCode.ADDRESS_NOT_FOUND);
            }
            CheckoutTemporaryLocationResponse location = response.data();
            ApiResponse<CurrentUserResponse> currentUserResponse = userServiceClient.getCurrentUser(bearer);
            if (currentUserResponse == null || !currentUserResponse.success()
                    || currentUserResponse.data() == null
                    || currentUserResponse.data().id() == null) {
                throw new AppException(ErrorCode.USER_SERVICE_UNAVAILABLE);
            }
            CurrentUserResponse currentUser = currentUserResponse.data();
            String recipientName = firstNonBlank(currentUser.fullName(), currentUser.username(), currentUser.email());
            String recipientPhone = firstNonBlank(currentUser.phoneNumber(), currentUser.email());
            if (recipientName == null || recipientPhone == null) {
                throw new AppException(ErrorCode.USER_SERVICE_UNAVAILABLE);
            }
            String formattedAddress = firstNonBlank(
                    location.formattedAddress(),
                    com.khanh.fooddelivery.order_service.common.address.AddressFormatter.format(
                            location.addressLine(), location.ward(), location.district(), location.city()));
            return new CheckoutPreviewResponse.CheckoutAddressSnapshot(
                    "TEMPORARY_LOCATION", null, location.id(), "TEMPORARY", null, "Vị trí hiện tại",
                    recipientName, recipientPhone, location.addressLine(), location.ward(), location.district(), location.city(),
                    location.latitude(), location.longitude(), null, null, null, null, null, formattedAddress);
        } catch (FeignException.NotFound exception) {
            throw new AppException(ErrorCode.ADDRESS_NOT_FOUND);
        } catch (FeignException exception) {
            throw new AppException(ErrorCode.USER_SERVICE_UNAVAILABLE);
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private RestaurantBranchCartAvailabilityResponse requireRestaurant(
            String bearer, UUID restaurantId, UUID branchId) {
        try {
            ApiResponse<RestaurantBranchCartAvailabilityResponse> response =
                    restaurantServiceClient.getCartAvailability(bearer, restaurantId, branchId);
            if (response == null || !response.success() || response.data() == null) {
                throw new AppException(ErrorCode.RESTAURANT_SERVICE_UNAVAILABLE);
            }
            RestaurantBranchCartAvailabilityResponse branch = response.data();
            if (!branch.restaurantActive() || !branch.branchActive() || !branch.acceptingOrders()) {
                throw new AppException(ErrorCode.BRANCH_NOT_ACCEPTING_ORDERS);
            }
            return branch;
        } catch (FeignException.NotFound exception) {
            throw new AppException(ErrorCode.BRANCH_NOT_ACCEPTING_ORDERS);
        } catch (FeignException exception) {
            throw new AppException(ErrorCode.RESTAURANT_SERVICE_UNAVAILABLE);
        }
    }

    private List<ValidatedCheckoutItemResponse> requireCatalogValidation(
            String bearer, InternalCartSnapshotResponse cart) {
        try {
            CheckoutItemsValidationRequest request =
                    new CheckoutItemsValidationRequest(
                            cart.restaurantId(),
                            cart.branchId(),
                            cart.items().stream()
                                    .map(item -> new CheckoutItemRequest(
                                            item.cartItemId(), item.catalogItemId(),
                                            item.selectedOptions().stream()
                                                    .map(InternalSelectedOptionSnapshotResponse::optionValueId)
                                                    .toList()))
                                    .toList());
            ApiResponse<CheckoutItemsValidationResponse> response =
                    catalogServiceClient.validateCheckoutItems(bearer, request);
            if (response == null || !response.success() || response.data() == null || response.data().items() == null) {
                throw new AppException(ErrorCode.CATALOG_SERVICE_UNAVAILABLE);
            }
            return response.data().items();
        } catch (FeignException exception) {
            throw catalogFailure(exception);
        }
    }

    private AppException catalogFailure(FeignException exception) {
        if (exception.status() == 400) return new AppException(ErrorCode.INVALID_OPTION_SELECTION);
        if (exception.status() == 404 || exception.status() == 409) return new AppException(ErrorCode.ITEM_UNAVAILABLE);
        return new AppException(ErrorCode.CATALOG_SERVICE_UNAVAILABLE);
    }

    private DeliveryResolution resolveDelivery(
            String bearer, UUID branchId, CheckoutPreviewResponse.CheckoutAddressSnapshot address, String currency) {
        if (address.latitude() == null || address.longitude() == null) {
            return DeliveryResolution.of(DeliveryQuoteStatus.LOCATION_REQUIRED);
        }
        try {
            ApiResponse<DeliveryQuoteResponse> response =
                    deliveryServiceClient.createQuote(bearer, new DeliveryQuoteRequest(branchId,
                            new DeliveryTargetRequest(address.targetType(), address.addressId(), address.temporaryLocationId())));
            if (response == null || !response.success() || response.data() == null) {
                return DeliveryResolution.of(DeliveryQuoteStatus.TEMPORARILY_UNAVAILABLE);
            }
            DeliveryQuoteResponse quote = response.data();
            if (!currency.equals(quote.currency()) || !quote.serviceable() || quote.deliveryFee() == null
                    || quote.quoteId() == null || quote.expiresAt() == null) {
                return DeliveryResolution.of(DeliveryQuoteStatus.TEMPORARILY_UNAVAILABLE);
            }
            return DeliveryResolution.available(quote);
        } catch (FeignException exception) {
            if (exception.status() == 409) return DeliveryResolution.of(DeliveryQuoteStatus.NOT_SERVICEABLE);
            if (exception.status() == 422) return DeliveryResolution.of(DeliveryQuoteStatus.LOCATION_REQUIRED);
            return DeliveryResolution.of(DeliveryQuoteStatus.TEMPORARILY_UNAVAILABLE);
        }
    }

    private record DeliveryResolution(DeliveryQuoteStatus status, DeliveryQuoteResponse quote) {
        static DeliveryResolution of(DeliveryQuoteStatus status) { return new DeliveryResolution(status, null); }
        static DeliveryResolution available(DeliveryQuoteResponse quote) {
            return new DeliveryResolution(DeliveryQuoteStatus.AVAILABLE, quote);
        }
        UUID quoteId() { return quote == null ? null : quote.quoteId(); }
        BigDecimal deliveryFee() { return quote == null ? null : quote.deliveryFee(); }
        Instant expiresAt() { return quote == null ? null : quote.expiresAt(); }
        String pricingPolicyVersion() { return quote == null ? null : quote.pricingPolicyVersion(); }
    }

    private Map<UUID, ValidatedCheckoutItemResponse> indexValidated(
            InternalCartSnapshotResponse cart,
            List<ValidatedCheckoutItemResponse> validated) {
        if (validated.size() != cart.items().size()) throw new AppException(ErrorCode.CATALOG_SERVICE_UNAVAILABLE);
        Map<UUID, ValidatedCheckoutItemResponse> byCartItem = new HashMap<>();
        for (ValidatedCheckoutItemResponse item : validated) {
            if (item == null || item.cartItemId() == null || byCartItem.put(item.cartItemId(), item) != null) {
                throw new AppException(ErrorCode.CATALOG_SERVICE_UNAVAILABLE);
            }
        }
        if (!byCartItem.keySet().equals(new HashSet<>(cart.items().stream()
                .map(InternalCartItemSnapshotResponse::cartItemId).toList()))) {
            throw new AppException(ErrorCode.CATALOG_SERVICE_UNAVAILABLE);
        }
        return byCartItem;
    }

    private String resolveCurrency(List<ValidatedCheckoutItemResponse> validated) {
        String currency = validated.getFirst().currency();
        if (currency == null || currency.isBlank() || validated.stream().anyMatch(item -> !currency.equals(item.currency()))) {
            throw new AppException(ErrorCode.CURRENCY_MISMATCH);
        }
        return currency;
    }

    private CheckoutPreviewResponse.CheckoutItemResponse toPreviewItem(
            InternalCartItemSnapshotResponse cartItem,
            ValidatedCheckoutItemResponse validated) {
        BigDecimal lineTotal = validated.finalUnitPrice().multiply(BigDecimal.valueOf(cartItem.quantity()));
        return new CheckoutPreviewResponse.CheckoutItemResponse(
                cartItem.cartItemId(), validated.catalogItemId(), validated.branchItemId(), validated.itemName(),
                validated.primaryImageUrl(), cartItem.quantity(), cartItem.note(),
                validated.selectedOptions().stream()
                        .map(option -> new CheckoutPreviewResponse.SelectedOptionResponse(
                                option.optionGroupId(), option.optionValueId(), option.groupName(), option.valueName(),
                                option.additionalPrice()))
                        .toList(),
                validated.sellingPrice(), validated.optionUnitPrice(), validated.finalUnitPrice(),
                validated.originalPrice(), lineTotal);
    }

    private CheckoutPreviewResponse.PriceChangeResponse priceChange(
            InternalCartItemSnapshotResponse cartItem,
            ValidatedCheckoutItemResponse validated) {
        if (cartItem.unitPrice() != null && cartItem.unitPrice().compareTo(validated.finalUnitPrice()) == 0) return null;
        return new CheckoutPreviewResponse.PriceChangeResponse(
                cartItem.cartItemId(), cartItem.catalogItemId(), validated.itemName(),
                cartItem.unitPrice(), validated.finalUnitPrice());
    }
}
