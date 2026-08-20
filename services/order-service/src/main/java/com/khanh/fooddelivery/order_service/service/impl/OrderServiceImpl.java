package com.khanh.fooddelivery.order_service.service.impl;

import com.khanh.fooddelivery.order_service.client.CartServiceClient;
import com.khanh.fooddelivery.order_service.client.DeliveryServiceClient;
import com.khanh.fooddelivery.order_service.client.dto.request.DeliveryMatchingRequest;
import com.khanh.fooddelivery.order_service.dto.request.CheckoutPreviewRequest;
import com.khanh.fooddelivery.order_service.dto.request.CreateOrderRequest;
import com.khanh.fooddelivery.order_service.dto.request.RejectOrderRequest;
import com.khanh.fooddelivery.order_service.dto.response.CheckoutPreviewResponse;
import com.khanh.fooddelivery.order_service.dto.response.OrderResponse;
import com.khanh.fooddelivery.order_service.entity.Order;
import com.khanh.fooddelivery.order_service.exception.AppException;
import com.khanh.fooddelivery.order_service.exception.ErrorCode;
import com.khanh.fooddelivery.order_service.mapper.OrderMapper;
import com.khanh.fooddelivery.order_service.repository.OrderRepository;
import com.khanh.fooddelivery.order_service.security.CurrentBearerTokenProvider;
import com.khanh.fooddelivery.order_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.order_service.service.CheckoutPreviewService;
import com.khanh.fooddelivery.order_service.service.OrderLifecycle;
import com.khanh.fooddelivery.order_service.service.OrderService;
import com.khanh.fooddelivery.order_service.service.OrderSnapshotFactory;
import com.khanh.fooddelivery.order_service.service.RestaurantAuthorizationService;
import feign.FeignException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orders;
    private final CheckoutPreviewService checkoutPreviewService;
    private final CurrentUserProvider currentUserProvider;
    private final CurrentBearerTokenProvider bearerTokenProvider;
    private final CartServiceClient cartClient;
    private final DeliveryServiceClient deliveryClient;
    private final OrderMapper orderMapper;
    private final OrderSnapshotFactory orderSnapshotFactory;
    private final RestaurantAuthorizationService restaurantAuthorization;

    @Override
    public OrderResponse create(
            Jwt jwt,
            CreateOrderRequest request
    ) {
        UUID customerId =
                currentUserProvider.getCurrentUserId(jwt);

        CheckoutPreviewResponse preview =
                checkoutPreviewService.preview(
                        jwt,
                        new CheckoutPreviewRequest(
                                request.branchId(),
                                request.cartVersion(),
                                request.target()
                        )
                );

        validatePlaceable(preview);

        Order order =
                orderSnapshotFactory.create(
                        customerId,
                        preview
                );

        orders.saveAndFlush(order);

        try {
            cartClient.clear(
                    bearerTokenProvider.getBearerToken(),
                    request.branchId()
            );
        } catch (FeignException exception) {
            throw new AppException(
                    ErrorCode.CART_SERVICE_UNAVAILABLE
            );
        }

        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> mine(Jwt jwt) {
        return orders
                .findByCustomerIdOrderByCreatedAtDesc(
                        currentUserProvider.getCurrentUserId(jwt)
                )
                .stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> restaurant(
            Jwt jwt,
            UUID restaurantId
    ) {
        restaurantAuthorization.requireAccess(
                restaurantId,
                null
        );

        return orders
                .findByRestaurantIdOrderByCreatedAtDesc(
                        restaurantId
                )
                .stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    public OrderResponse accept(
            Jwt jwt,
            UUID orderId
    ) {
        Order order =
                restaurantOrder(
                        jwt,
                        orderId
                );
        OrderLifecycle.accept(order);
        orders.saveAndFlush(order);
        try {
            deliveryClient.startMatching(
                    bearerTokenProvider.getBearerToken(),
                    new DeliveryMatchingRequest(
                            order.getId(),
                            order.getRestaurantId(),
                            order.getBranchId(),
                            order.getCustomerId(),
                            order.getRestaurantName(),
                            order.getBranchName(),
                            order.getAddressDisplayLabel(),
                            order.getLatitude(),
                            order.getLongitude()
                    )
            );
        } catch (FeignException exception) {
            throw new AppException(
                    ErrorCode.DELIVERY_SERVICE_UNAVAILABLE
            );
        }
        return orderMapper.toResponse(order);
    }

    @Override
    public OrderResponse reject(
            Jwt jwt,
            UUID orderId,
            RejectOrderRequest request
    ) {
        Order order =
                restaurantOrder(
                        jwt,
                        orderId
                );

        OrderLifecycle.reject(order);
        order.setRejectionReason(
                normalizeReason(request.reason())
        );
        return orderMapper.toResponse(order);
    }

    @Override
    public void deliveryAssigned(UUID orderId) {
        OrderLifecycle.preparing(
                require(orderId)
        );
    }

    @Override
    public void pickedUp(UUID orderId) {
        OrderLifecycle.delivering(
                require(orderId)
        );
    }

    @Override
    public void delivered(UUID orderId) {
        OrderLifecycle.completed(
                require(orderId)
        );
    }

    @Override
    public void matchingFailed(UUID orderId) {
        OrderLifecycle.cancelMatching(
                require(orderId)
        );
    }

    private void validatePlaceable(
            CheckoutPreviewResponse preview
    ) {
        if (!preview.canPlaceOrder()
                || preview.totalAmount() == null
                || preview.deliveryFee() == null) {
            throw new AppException(
                    ErrorCode.ORDER_NOT_PLACEABLE
            );
        }
    }

    private Order restaurantOrder(
            Jwt jwt,
            UUID orderId
    ) {
        Order order = require(orderId);

        restaurantAuthorization.requireAccess(
                order.getRestaurantId(),
                order.getBranchId()
        );

        return order;
    }

    private Order require(UUID orderId) {
        return orders
                .findWithItemsById(orderId)
                .orElseThrow(
                        () -> new AppException(
                                ErrorCode.ORDER_NOT_FOUND
                        )
                );
    }

    private String normalizeReason(String reason) {
        return reason == null || reason.isBlank()
                ? null
                : reason.trim();
    }
}