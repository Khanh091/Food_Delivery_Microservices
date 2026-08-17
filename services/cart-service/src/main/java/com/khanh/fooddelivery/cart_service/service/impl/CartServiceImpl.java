package com.khanh.fooddelivery.cart_service.service.impl;

import com.khanh.fooddelivery.cart_service.client.CatalogServiceClient;
import com.khanh.fooddelivery.cart_service.client.RestaurantServiceClient;
import com.khanh.fooddelivery.cart_service.config.CartProperties;
import com.khanh.fooddelivery.cart_service.dto.request.AddCartItemRequest;
import com.khanh.fooddelivery.cart_service.dto.request.ReplaceCartItemRequest;
import com.khanh.fooddelivery.cart_service.dto.request.UpdateCartItemQuantityRequest;
import com.khanh.fooddelivery.cart_service.dto.response.CartItemResponse;
import com.khanh.fooddelivery.cart_service.dto.response.CartResponse;
import com.khanh.fooddelivery.cart_service.dto.response.SelectedOptionResponse;
import com.khanh.fooddelivery.cart_service.dto.response.internal.InternalCartItemSnapshotResponse;
import com.khanh.fooddelivery.cart_service.dto.response.internal.InternalCartSnapshotResponse;
import com.khanh.fooddelivery.cart_service.dto.response.internal.InternalSelectedOptionSnapshotResponse;
import com.khanh.fooddelivery.cart_service.exception.AppException;
import com.khanh.fooddelivery.cart_service.exception.ErrorCode;
import com.khanh.fooddelivery.cart_service.model.Cart;
import com.khanh.fooddelivery.cart_service.model.CartItem;
import com.khanh.fooddelivery.cart_service.model.SelectedOption;
import com.khanh.fooddelivery.cart_service.repository.CartRepository;
import com.khanh.fooddelivery.cart_service.repository.CartSnapshot;
import com.khanh.fooddelivery.cart_service.security.CurrentBearerTokenProvider;
import com.khanh.fooddelivery.cart_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.cart_service.service.CartFingerprint;
import com.khanh.fooddelivery.cart_service.service.CartService;
import feign.FeignException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private static final int MAX_QUANTITY = 99;
    private static final int SCHEMA_VERSION = 1;

    private final CartRepository carts;
    private final CatalogServiceClient catalogServiceClient;
    private final RestaurantServiceClient restaurantServiceClient;
    private final CurrentUserProvider currentUserProvider;
    private final CurrentBearerTokenProvider bearerTokenProvider;
    private final CartProperties properties;

    @Override
    public CartResponse get(Jwt jwt) {
        UUID ownerUserId = currentUserProvider.getCurrentUserId(jwt);
        return carts.find(ownerUserId).map(this::toResponse).orElseGet(CartServiceImpl::emptyResponse);
    }

    @Override
    public InternalCartSnapshotResponse getInternalSnapshot(Jwt jwt) {
        UUID ownerUserId = currentUserProvider.getCurrentUserId(jwt);
        return carts.find(ownerUserId)
                .map(CartSnapshot::cart)
                .map(this::toInternalSnapshot)
                .orElseGet(() -> new InternalCartSnapshotResponse(
                        ownerUserId, null, null, null, List.of(), 0, null, null));
    }

    @Override
    public CartResponse add(Jwt jwt, AddCartItemRequest request) {
        UUID ownerUserId = currentUserProvider.getCurrentUserId(jwt);
        ValidatedAdd validated = validate(request);
        for (int attempt = 0; attempt < properties.casMaxRetries(); attempt++) {
            Optional<CartSnapshot> current = carts.find(ownerUserId);
            Cart existing = current.map(CartSnapshot::cart).orElse(null);
            requireSameBranch(existing, request.restaurantId(), request.branchId());
            Cart candidate = addToCart(ownerUserId, existing, request, validated);
            long expectedVersion = existing == null ? 0 : existing.version();
            if (carts.compareAndSet(ownerUserId, expectedVersion, candidate)) {
                return toResponse(candidate, Instant.now().plus(properties.ttl()));
            }
        }
        throw new AppException(ErrorCode.CART_VERSION_CONFLICT);
    }

    @Override
    public CartResponse replace(Jwt jwt, ReplaceCartItemRequest request) {
        UUID ownerUserId = currentUserProvider.getCurrentUserId(jwt);
        CartSnapshot current = carts.find(ownerUserId).orElseThrow(() -> new AppException(ErrorCode.CART_VERSION_CONFLICT));
        if (current.cart().version() != request.expectedCartVersion()) {
            throw new AppException(ErrorCode.CART_VERSION_CONFLICT);
        }
        ValidatedAdd validated = validate(request.item());
        Cart candidate = addToCart(ownerUserId, null, request.item(), validated, current.cart().version() + 1);
        if (!carts.compareAndSet(ownerUserId, current.cart().version(), candidate)) {
            throw new AppException(ErrorCode.CART_VERSION_CONFLICT);
        }
        return toResponse(candidate, Instant.now().plus(properties.ttl()));
    }

    @Override
    public CartResponse updateQuantity(Jwt jwt, UUID cartItemId, UpdateCartItemQuantityRequest request) {
        UUID ownerUserId = currentUserProvider.getCurrentUserId(jwt);
        for (int attempt = 0; attempt < properties.casMaxRetries(); attempt++) {
            CartSnapshot current = carts.find(ownerUserId).orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));
            CartItem existingItem =
                    current.cart().items().stream()
                            .filter(item -> item.id().equals(cartItemId))
                            .findFirst()
                            .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));
            ValidatedAdd validated =
                    validate(
                            new AddCartItemRequest(
                                    current.cart().restaurantId(),
                                    current.cart().branchId(),
                                    existingItem.catalogItemId(),
                                    request.quantity(),
                                    existingItem.selectedOptions().stream()
                                            .map(SelectedOption::optionValueId)
                                            .toList(),
                                    existingItem.note()));
            Cart candidate = replaceItem(current.cart(), existingItem, request.quantity(), validated);
            if (carts.compareAndSet(ownerUserId, current.cart().version(), candidate)) {
                return toResponse(candidate, Instant.now().plus(properties.ttl()));
            }
        }
        throw new AppException(ErrorCode.CART_VERSION_CONFLICT);
    }

    @Override
    public CartResponse remove(Jwt jwt, UUID cartItemId) {
        UUID ownerUserId = currentUserProvider.getCurrentUserId(jwt);
        for (int attempt = 0; attempt < properties.casMaxRetries(); attempt++) {
            CartSnapshot current = carts.find(ownerUserId).orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));
            List<CartItem> remaining =
                    current.cart().items().stream().filter(item -> !item.id().equals(cartItemId)).toList();
            if (remaining.size() == current.cart().items().size()) {
                throw new AppException(ErrorCode.CART_ITEM_NOT_FOUND);
            }
            if (remaining.isEmpty()) {
                if (carts.compareAndDelete(ownerUserId, current.cart().version())) return emptyResponse();
            } else {
                Cart candidate = copyCart(current.cart(), remaining, current.cart().version() + 1, Instant.now());
                if (carts.compareAndSet(ownerUserId, current.cart().version(), candidate)) {
                    return toResponse(candidate, Instant.now().plus(properties.ttl()));
                }
            }
        }
        throw new AppException(ErrorCode.CART_VERSION_CONFLICT);
    }

    @Override
    public CartResponse clear(Jwt jwt) {
        UUID ownerUserId = currentUserProvider.getCurrentUserId(jwt);
        for (int attempt = 0; attempt < properties.casMaxRetries(); attempt++) {
            Optional<CartSnapshot> current = carts.find(ownerUserId);
            if (current.isEmpty()) return emptyResponse();
            if (carts.compareAndDelete(ownerUserId, current.get().cart().version())) return emptyResponse();
        }
        throw new AppException(ErrorCode.CART_VERSION_CONFLICT);
    }

    private ValidatedAdd validate(AddCartItemRequest request) {
        String bearer = bearerTokenProvider.getBearerToken();
        RestaurantServiceClient.RestaurantBranchCartAvailabilityResponse restaurant =
                requireRestaurantAvailability(bearer, request.restaurantId(), request.branchId());
        CatalogServiceClient.CartItemValidationResponse catalog =
                requireCatalogValidation(bearer, request);
        return new ValidatedAdd(restaurant, catalog, normalizeNote(request.note()));
    }

    private RestaurantServiceClient.RestaurantBranchCartAvailabilityResponse requireRestaurantAvailability(
            String bearer, UUID restaurantId, UUID branchId) {
        try {
            RestaurantServiceClient.ApiResponse<RestaurantServiceClient.RestaurantBranchCartAvailabilityResponse>
                    response = restaurantServiceClient.getCartAvailability(bearer, restaurantId, branchId);
            if (response == null || !response.success() || response.data() == null) {
                throw new AppException(ErrorCode.RESTAURANT_SERVICE_UNAVAILABLE);
            }
            RestaurantServiceClient.RestaurantBranchCartAvailabilityResponse data = response.data();
            if (!data.restaurantActive() || !data.branchActive() || !data.acceptingOrders()) {
                throw new AppException(ErrorCode.BRANCH_NOT_ACCEPTING_ORDERS);
            }
            return data;
        } catch (FeignException.NotFound exception) {
            throw new AppException(ErrorCode.BRANCH_NOT_ACCEPTING_ORDERS);
        } catch (FeignException exception) {
            throw new AppException(ErrorCode.RESTAURANT_SERVICE_UNAVAILABLE);
        }
    }

    private CatalogServiceClient.CartItemValidationResponse requireCatalogValidation(
            String bearer, AddCartItemRequest request) {
        try {
            CatalogServiceClient.ApiResponse<CatalogServiceClient.CartItemValidationResponse> response =
                    catalogServiceClient.validateCartItem(
                            bearer,
                            new CatalogServiceClient.CartItemValidationRequest(
                                    request.restaurantId(),
                                    request.branchId(),
                                    request.catalogItemId(),
                                    request.selectedOptionValueIds()));
            if (response == null || !response.success() || response.data() == null) {
                throw new AppException(ErrorCode.CATALOG_SERVICE_UNAVAILABLE);
            }
            return response.data();
        } catch (FeignException exception) {
            throw catalogFailure(exception);
        }
    }

    private AppException catalogFailure(FeignException exception) {
        String body = exception.contentUTF8();
        if (exception.status() == 404) {
            return new AppException(
                    body.contains("CATALOG_016")
                            ? ErrorCode.BRANCH_ITEM_NOT_FOUND
                            : ErrorCode.CATALOG_ITEM_NOT_FOUND);
        }
        if (exception.status() == 400) return new AppException(ErrorCode.INVALID_OPTION_SELECTION);
        if (exception.status() == 409) return new AppException(ErrorCode.ITEM_UNAVAILABLE);
        return new AppException(ErrorCode.CATALOG_SERVICE_UNAVAILABLE);
    }

    private Cart addToCart(UUID ownerUserId, Cart existing, AddCartItemRequest request, ValidatedAdd validated) {
        return addToCart(ownerUserId, existing, request, validated, existing == null ? 1 : existing.version() + 1);
    }

    private Cart addToCart(
            UUID ownerUserId,
            Cart existing,
            AddCartItemRequest request,
            ValidatedAdd validated,
            long version) {
        CartItem newItem = newItem(request, validated);
        Instant now = Instant.now();
        if (existing == null) {
            return new Cart(
                    SCHEMA_VERSION,
                    ownerUserId,
                    request.restaurantId(),
                    request.branchId(),
                    validated.restaurant().restaurantName(),
                    validated.restaurant().branchName(),
                    validated.catalog().currency(),
                    List.of(newItem),
                    version,
                    now,
                    now);
        }
        List<CartItem> items = new ArrayList<>(existing.items());
        for (int index = 0; index < items.size(); index++) {
            CartItem item = items.get(index);
            if (item.configurationFingerprint().equals(newItem.configurationFingerprint())) {
                int mergedQuantity = item.quantity() + request.quantity();
                requireQuantity(mergedQuantity);
                items.set(index, newItem(item.id(), mergedQuantity, request, validated));
                return copyCart(existing, List.copyOf(items), version, now);
            }
        }
        items.add(newItem);
        return copyCart(existing, List.copyOf(items), version, now);
    }

    private Cart replaceItem(Cart cart, CartItem oldItem, int quantity, ValidatedAdd validated) {
        List<CartItem> items =
                cart.items().stream()
                        .map(
                                item ->
                                        item.id().equals(oldItem.id())
                                                ? newItem(item.id(), quantity, oldItem.note(), oldItem.selectedOptions(), validated)
                                                : item)
                        .toList();
        return copyCart(cart, items, cart.version() + 1, Instant.now());
    }

    private CartItem newItem(AddCartItemRequest request, ValidatedAdd validated) {
        return newItem(UUID.randomUUID(), request.quantity(), request, validated);
    }

    private CartItem newItem(UUID id, int quantity, AddCartItemRequest request, ValidatedAdd validated) {
        String note = validated.note();
        return newItem(
                id,
                quantity,
                note,
                selectedOptions(validated.catalog()),
                validated.catalog(),
                CartFingerprint.of(request.catalogItemId(), request.selectedOptionValueIds(), note));
    }

    private CartItem newItem(
            UUID id,
            int quantity,
            String note,
            List<SelectedOption> priorSelectedOptions,
            ValidatedAdd validated) {
        List<UUID> optionIds = priorSelectedOptions.stream().map(SelectedOption::optionValueId).toList();
        return newItem(
                id,
                quantity,
                validated.note(),
                selectedOptions(validated.catalog()),
                validated.catalog(),
                CartFingerprint.of(validated.catalog().catalogItemId(), optionIds, validated.note()));
    }

    private CartItem newItem(
            UUID id,
            int quantity,
            String note,
            List<SelectedOption> options,
            CatalogServiceClient.CartItemValidationResponse catalog,
            String fingerprint) {
        return new CartItem(
                id,
                fingerprint,
                catalog.catalogItemId(),
                catalog.branchItemId(),
                quantity,
                note,
                options,
                catalog.itemName(),
                catalog.primaryImageUrl(),
                catalog.sellingPrice(),
                catalog.optionUnitPrice(),
                catalog.finalUnitPrice(),
                catalog.originalPrice());
    }

    private List<SelectedOption> selectedOptions(CatalogServiceClient.CartItemValidationResponse catalog) {
        return catalog.selectedOptions().stream()
                .map(
                        option ->
                                new SelectedOption(
                                        option.optionGroupId(),
                                        option.optionValueId(),
                                        option.groupName(),
                                        option.valueName(),
                                        option.additionalPrice()))
                .toList();
    }

    private Cart copyCart(Cart source, List<CartItem> items, long version, Instant updatedAt) {
        return new Cart(
                source.schemaVersion(),
                source.ownerUserId(),
                source.restaurantId(),
                source.branchId(),
                source.restaurantNameSnapshot(),
                source.branchNameSnapshot(),
                source.currency(),
                items,
                version,
                source.createdAt(),
                updatedAt);
    }

    private void requireSameBranch(Cart cart, UUID restaurantId, UUID branchId) {
        if (cart == null) return;
        if (!cart.restaurantId().equals(restaurantId) || !cart.branchId().equals(branchId)) {
            throw new AppException(
                    ErrorCode.CART_DIFFERENT_BRANCH,
                    "Cart already contains items from " + cart.restaurantNameSnapshot() + " - " + cart.branchNameSnapshot());
        }
    }

    private void requireQuantity(int quantity) {
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new AppException(ErrorCode.QUANTITY_OUT_OF_RANGE);
        }
    }

    private String normalizeNote(String note) {
        if (note == null) return null;
        String normalized = note.trim();
        if (normalized.length() > 500) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Note must not exceed 500 characters");
        }
        return normalized.isEmpty() ? null : normalized;
    }

    private CartResponse toResponse(CartSnapshot snapshot) {
        return toResponse(snapshot.cart(), snapshot.expiresAt());
    }

    private CartResponse toResponse(Cart cart, Instant expiresAt) {
        List<CartItemResponse> items =
                cart.items().stream().map(this::toResponse).toList();
        BigDecimal subtotal =
                items.stream().map(CartItemResponse::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalQuantity = items.stream().mapToInt(CartItemResponse::quantity).sum();
        return new CartResponse(
                cart.restaurantId(),
                cart.restaurantNameSnapshot(),
                cart.branchId(),
                cart.branchNameSnapshot(),
                cart.currency(),
                items,
                subtotal,
                totalQuantity,
                cart.version(),
                cart.createdAt(),
                cart.updatedAt(),
                expiresAt);
    }

    private CartItemResponse toResponse(CartItem item) {
        return new CartItemResponse(
                item.id(),
                item.catalogItemId(),
                item.branchItemId(),
                item.itemNameSnapshot(),
                item.imageUrlSnapshot(),
                item.quantity(),
                item.note(),
                item.selectedOptions().stream()
                        .map(
                                option ->
                                        new SelectedOptionResponse(
                                                option.optionGroupId(),
                                                option.optionValueId(),
                                                option.groupNameSnapshot(),
                                                option.valueNameSnapshot(),
                                                option.additionalPriceSnapshot()))
                        .toList(),
                item.baseUnitPriceSnapshot(),
                item.optionUnitPriceSnapshot(),
                item.unitPriceSnapshot(),
                item.originalPriceSnapshot(),
                item.lineTotal());
    }

    private InternalCartSnapshotResponse toInternalSnapshot(Cart cart) {
        return new InternalCartSnapshotResponse(
                cart.ownerUserId(),
                cart.restaurantId(),
                cart.branchId(),
                cart.currency(),
                cart.items().stream()
                        .map(
                                item ->
                                        new InternalCartItemSnapshotResponse(
                                                item.id(),
                                                item.catalogItemId(),
                                                item.branchItemId(),
                                                item.quantity(),
                                                item.note(),
                                                item.selectedOptions().stream()
                                                        .map(
                                                                option ->
                                                                        new InternalSelectedOptionSnapshotResponse(
                                                                                option.optionGroupId(),
                                                                                option.optionValueId(),
                                                                                option.groupNameSnapshot(),
                                                                                option.valueNameSnapshot(),
                                                                                option.additionalPriceSnapshot()))
                                                        .toList(),
                                                item.itemNameSnapshot(),
                                                item.imageUrlSnapshot(),
                                                item.baseUnitPriceSnapshot(),
                                                item.optionUnitPriceSnapshot(),
                                                item.unitPriceSnapshot(),
                                                item.originalPriceSnapshot()))
                        .toList(),
                cart.version(),
                cart.createdAt(),
                cart.updatedAt());
    }

    private static CartResponse emptyResponse() {
        return new CartResponse(
                null,
                null,
                null,
                null,
                null,
                List.of(),
                BigDecimal.ZERO,
                0,
                0,
                null,
                null,
                null);
    }

    private record ValidatedAdd(
            RestaurantServiceClient.RestaurantBranchCartAvailabilityResponse restaurant,
            CatalogServiceClient.CartItemValidationResponse catalog,
            String note) {}
}
