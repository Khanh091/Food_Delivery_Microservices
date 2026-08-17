package com.khanh.fooddelivery.order_service.service.impl;

import com.khanh.fooddelivery.order_service.client.CartServiceClient;
import com.khanh.fooddelivery.order_service.client.CatalogServiceClient;
import com.khanh.fooddelivery.order_service.client.RemoteApiResponse;
import com.khanh.fooddelivery.order_service.client.RestaurantServiceClient;
import com.khanh.fooddelivery.order_service.client.UserServiceClient;
import com.khanh.fooddelivery.order_service.dto.request.CheckoutPreviewRequest;
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
    private final CurrentUserProvider currentUserProvider;
    private final CurrentBearerTokenProvider bearerTokenProvider;
    private final CheckoutPreviewFingerprint fingerprint;

    @Override
    public CheckoutPreviewResponse preview(Jwt jwt, CheckoutPreviewRequest request) {
        UUID ownerUserId = currentUserProvider.getCurrentUserId(jwt);
        String bearer = bearerTokenProvider.getBearerToken();
        CartServiceClient.InternalCartSnapshotResponse cart = requireCart(bearer, request.branchId());
        validateCart(ownerUserId, cart, request.branchId(), request.cartVersion());
        CheckoutPreviewResponse.CheckoutAddressSnapshot address = requireAddress(bearer, request.addressId());
        RestaurantServiceClient.RestaurantBranchCartAvailabilityResponse branch =
                requireRestaurant(bearer, cart.restaurantId(), cart.branchId());
        List<CatalogServiceClient.ValidatedCheckoutItemResponse> validated =
                requireCatalogValidation(bearer, cart);
        Map<UUID, CatalogServiceClient.ValidatedCheckoutItemResponse> validatedByCartItem = indexValidated(cart, validated);
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
        String previewFingerprint = fingerprint.of(
                ownerUserId, cart.version(), address, restaurant, branchSnapshot, items, currency, subtotal,
                discountAmount, DeliveryQuoteStatus.NOT_AVAILABLE);
        return new CheckoutPreviewResponse(
                cart.version(), address, restaurant, branchSnapshot, items, currency, subtotal, discountAmount,
                DeliveryQuoteStatus.NOT_AVAILABLE, null, null, priceChanges, previewFingerprint, Instant.now(), false);
    }

    private CartServiceClient.InternalCartSnapshotResponse requireCart(String bearer, UUID branchId) {
        try {
            RemoteApiResponse<CartServiceClient.InternalCartSnapshotResponse> response =
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
            UUID ownerUserId, CartServiceClient.InternalCartSnapshotResponse cart, UUID expectedBranchId, long expectedVersion) {
        if (!ownerUserId.equals(cart.ownerUserId())) throw new AppException(ErrorCode.ACCESS_DENIED);
        if (cart.items() == null || cart.items().isEmpty()) throw new AppException(ErrorCode.CART_EMPTY);
        if (!expectedBranchId.equals(cart.branchId())) throw new AppException(ErrorCode.CART_SERVICE_UNAVAILABLE);
        if (cart.version() != expectedVersion) throw new AppException(ErrorCode.CART_VERSION_CONFLICT);
        if (cart.restaurantId() == null || cart.branchId() == null) throw new AppException(ErrorCode.CART_EMPTY);
    }

    private CheckoutPreviewResponse.CheckoutAddressSnapshot requireAddress(String bearer, UUID addressId) {
        try {
            RemoteApiResponse<UserServiceClient.InternalUserAddressResponse> response =
                    userServiceClient.getOwnedAddress(bearer, addressId);
            if (response == null || !response.success() || response.data() == null) {
                throw new AppException(ErrorCode.USER_SERVICE_UNAVAILABLE);
            }
            UserServiceClient.InternalUserAddressResponse address = response.data();
            return new CheckoutPreviewResponse.CheckoutAddressSnapshot(
                    address.id(), address.labelType(), address.customLabel(), address.displayLabel(),
                    address.recipientName(), address.recipientPhone(), address.addressLine(), address.ward(),
                    address.district(), address.city(), address.latitude(), address.longitude(), address.buildingName(),
                    address.floor(), address.entrance(), address.deliveryNote(), address.version());
        } catch (FeignException.NotFound exception) {
            throw new AppException(ErrorCode.ADDRESS_NOT_FOUND);
        } catch (FeignException exception) {
            throw new AppException(ErrorCode.USER_SERVICE_UNAVAILABLE);
        }
    }

    private RestaurantServiceClient.RestaurantBranchCartAvailabilityResponse requireRestaurant(
            String bearer, UUID restaurantId, UUID branchId) {
        try {
            RemoteApiResponse<RestaurantServiceClient.RestaurantBranchCartAvailabilityResponse> response =
                    restaurantServiceClient.getCartAvailability(bearer, restaurantId, branchId);
            if (response == null || !response.success() || response.data() == null) {
                throw new AppException(ErrorCode.RESTAURANT_SERVICE_UNAVAILABLE);
            }
            RestaurantServiceClient.RestaurantBranchCartAvailabilityResponse branch = response.data();
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

    private List<CatalogServiceClient.ValidatedCheckoutItemResponse> requireCatalogValidation(
            String bearer, CartServiceClient.InternalCartSnapshotResponse cart) {
        try {
            CatalogServiceClient.CheckoutItemsValidationRequest request =
                    new CatalogServiceClient.CheckoutItemsValidationRequest(
                            cart.restaurantId(),
                            cart.branchId(),
                            cart.items().stream()
                                    .map(item -> new CatalogServiceClient.CheckoutItemRequest(
                                            item.cartItemId(), item.catalogItemId(),
                                            item.selectedOptions().stream()
                                                    .map(CartServiceClient.InternalSelectedOptionSnapshotResponse::optionValueId)
                                                    .toList()))
                                    .toList());
            RemoteApiResponse<CatalogServiceClient.CheckoutItemsValidationResponse> response =
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

    private Map<UUID, CatalogServiceClient.ValidatedCheckoutItemResponse> indexValidated(
            CartServiceClient.InternalCartSnapshotResponse cart,
            List<CatalogServiceClient.ValidatedCheckoutItemResponse> validated) {
        if (validated.size() != cart.items().size()) throw new AppException(ErrorCode.CATALOG_SERVICE_UNAVAILABLE);
        Map<UUID, CatalogServiceClient.ValidatedCheckoutItemResponse> byCartItem = new HashMap<>();
        for (CatalogServiceClient.ValidatedCheckoutItemResponse item : validated) {
            if (item == null || item.cartItemId() == null || byCartItem.put(item.cartItemId(), item) != null) {
                throw new AppException(ErrorCode.CATALOG_SERVICE_UNAVAILABLE);
            }
        }
        if (!byCartItem.keySet().equals(new HashSet<>(cart.items().stream()
                .map(CartServiceClient.InternalCartItemSnapshotResponse::cartItemId).toList()))) {
            throw new AppException(ErrorCode.CATALOG_SERVICE_UNAVAILABLE);
        }
        return byCartItem;
    }

    private String resolveCurrency(List<CatalogServiceClient.ValidatedCheckoutItemResponse> validated) {
        String currency = validated.getFirst().currency();
        if (currency == null || currency.isBlank() || validated.stream().anyMatch(item -> !currency.equals(item.currency()))) {
            throw new AppException(ErrorCode.CURRENCY_MISMATCH);
        }
        return currency;
    }

    private CheckoutPreviewResponse.CheckoutItemResponse toPreviewItem(
            CartServiceClient.InternalCartItemSnapshotResponse cartItem,
            CatalogServiceClient.ValidatedCheckoutItemResponse validated) {
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
            CartServiceClient.InternalCartItemSnapshotResponse cartItem,
            CatalogServiceClient.ValidatedCheckoutItemResponse validated) {
        if (cartItem.unitPrice() != null && cartItem.unitPrice().compareTo(validated.finalUnitPrice()) == 0) return null;
        return new CheckoutPreviewResponse.PriceChangeResponse(
                cartItem.cartItemId(), cartItem.catalogItemId(), validated.itemName(),
                cartItem.unitPrice(), validated.finalUnitPrice());
    }
}
