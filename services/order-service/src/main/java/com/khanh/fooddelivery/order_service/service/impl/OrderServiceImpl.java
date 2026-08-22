package com.khanh.fooddelivery.order_service.service.impl;

import com.khanh.fooddelivery.order_service.client.CartServiceClient;
import com.khanh.fooddelivery.order_service.client.PaymentServiceClient;
import com.khanh.fooddelivery.order_service.dto.request.CheckoutPreviewRequest;
import com.khanh.fooddelivery.order_service.dto.request.CreateOrderRequest;
import com.khanh.fooddelivery.order_service.dto.request.RejectOrderRequest;
import com.khanh.fooddelivery.order_service.dto.response.CheckoutPreviewResponse;
import com.khanh.fooddelivery.order_service.dto.response.OrderResponse;
import com.khanh.fooddelivery.order_service.entity.Order;
import com.khanh.fooddelivery.order_service.exception.AppException;
import com.khanh.fooddelivery.order_service.exception.ErrorCode;
import com.khanh.fooddelivery.order_service.enums.OrderStatus;
import com.khanh.fooddelivery.order_service.enums.PaymentStatus;
import com.khanh.fooddelivery.order_service.mapper.OrderMapper;
import com.khanh.fooddelivery.order_service.outbox.OrderOutboxService;
import com.khanh.fooddelivery.order_service.placement.OrderPlacementClaim;
import com.khanh.fooddelivery.order_service.placement.OrderPlacementIdempotencyService;
import com.khanh.fooddelivery.order_service.repository.OrderRepository;
import com.khanh.fooddelivery.order_service.security.CurrentBearerTokenProvider;
import com.khanh.fooddelivery.order_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.order_service.service.CheckoutPreviewService;
import com.khanh.fooddelivery.order_service.service.OrderLifecycle;
import com.khanh.fooddelivery.order_service.service.OrderService;
import com.khanh.fooddelivery.order_service.service.RestaurantAuthorizationService;
import feign.FeignException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orders;
    private final CheckoutPreviewService checkoutPreviewService;
    private final CurrentUserProvider currentUserProvider;
    private final CurrentBearerTokenProvider bearerTokenProvider;
    private final CartServiceClient cartClient;
    private final OrderMapper orderMapper;
    private final OrderPlacementRecoveryService orderPlacementRecoveryService;
    private final OrderCreationTransactionService orderCreationTransactionService;
    private final OrderPlacementIdempotencyService orderPlacementIdempotencyService;
    private final RestaurantAuthorizationService restaurantAuthorization;
    private final PaymentServiceClient paymentClient;
    private final OrderOutboxService orderOutbox;

    @Value("${app.internal-api.key:}")
    private String internalApiKey;

    @Override
    public OrderResponse create(
            Jwt jwt,
            CreateOrderRequest request,
            String idempotencyKey
    ) {
        UUID customerId =
                currentUserProvider.getCurrentUserId(jwt);

        OrderPlacementClaim claim = orderPlacementIdempotencyService.claim(
                customerId,
                idempotencyKey,
                request
        );
        if (claim.status() == OrderPlacementClaim.Status.COMPLETED) {
            return recoverCompletedPlacement(claim);
        }
        if (claim.status() == OrderPlacementClaim.Status.IN_PROGRESS) {
            OrderResponse recovered = findExistingOrder(claim.reservedOrderId());
            if (recovered != null) {
                orderPlacementIdempotencyService.recoverCompleted(
                        claim.requestId(),
                        claim.reservedOrderId()
                );
                clearCartAfterPlacement(claim.branchId(), claim.cartVersion());
                return recovered;
            }
            throw new AppException(ErrorCode.ORDER_IDEMPOTENCY_IN_PROGRESS);
        }

        OrderResponse recovered = findExistingOrder(claim.reservedOrderId());
        if (recovered != null) {
            orderPlacementIdempotencyService.markCompleted(claim.requestId(), claim.claimToken());
            clearCartAfterPlacement(claim.branchId(), claim.cartVersion());
            return recovered;
        }

        CheckoutPreviewResponse preview;
        try {
            preview = checkoutPreviewService.preview(
                    jwt,
                    new CheckoutPreviewRequest(
                            request.branchId(),
                            request.cartVersion(),
                            request.target()
                    )
            );
            validatePlaceable(preview);
        } catch (RuntimeException exception) {
            releasePlacementClaim(claim);
            throw exception;
        }

        OrderResponse response = orderCreationTransactionService.create(
                claim.reservedOrderId(),
                customerId,
                preview,
                request.effectivePaymentMethod()
        );
        orderPlacementIdempotencyService.markCompleted(claim.requestId(), claim.claimToken());
        clearCartAfterPlacement(claim.branchId(), claim.cartVersion());
        return response;
    }

    private OrderResponse recoverCompletedPlacement(OrderPlacementClaim claim) {
        OrderResponse response = findExistingOrder(claim.reservedOrderId());
        if (response == null) {
            throw new AppException(ErrorCode.ORDER_IDEMPOTENCY_RECOVERY_FAILED);
        }
        clearCartAfterPlacement(claim.branchId(), claim.cartVersion());
        return response;
    }

    private OrderResponse findExistingOrder(UUID orderId) {
        return orderPlacementRecoveryService.findResponse(orderId);
    }

    private void releasePlacementClaim(OrderPlacementClaim claim) {
        try {
            orderPlacementIdempotencyService.release(claim.requestId(), claim.claimToken());
        } catch (RuntimeException releaseFailure) {
            log.warn("Order placement claim release failed requestId={} reasonType={}",
                    claim.requestId(), releaseFailure.getClass().getSimpleName());
        }
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
                .filter(order -> order.getStatus() != com.khanh.fooddelivery.order_service.enums.OrderStatus.PENDING_PAYMENT)
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
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
        orderOutbox.publishOrderConfirmed(order);
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
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
        if (order.getPaymentStatus() == com.khanh.fooddelivery.order_service.enums.PaymentStatus.PAID) {
            try {
                var payment = paymentClient.refund(internalApiKey, order.getId());
                if (payment != null && payment.data() != null) {
                    order.setPaymentStatus(payment.data().status());
                }
            } catch (FeignException exception) {
                throw new AppException(ErrorCode.PAYMENT_SERVICE_UNAVAILABLE);
            }
        } else if (order.getPaymentMethod() == com.khanh.fooddelivery.order_service.enums.PaymentMethod.COD) {
            try {
                var payment = paymentClient.cancel(internalApiKey, order.getId());
                if (payment != null && payment.data() != null) {
                    order.setPaymentStatus(payment.data().status());
                }
            } catch (FeignException exception) {
                throw new AppException(ErrorCode.PAYMENT_SERVICE_UNAVAILABLE);
            }
        }
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public void paymentSucceeded(UUID orderId) {
        Order order = require(orderId);
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            if (order.getPaymentStatus() != PaymentStatus.PENDING
                    && order.getPaymentStatus() != PaymentStatus.PROCESSING
                    && order.getPaymentStatus() != PaymentStatus.PAID) {
                throw new AppException(ErrorCode.ORDER_TRANSITION_INVALID);
            }
            order.setPaymentStatus(PaymentStatus.PAID);
            OrderLifecycle.paymentSucceeded(order);
            return;
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return;
        }
        throw new AppException(ErrorCode.ORDER_TRANSITION_INVALID);
    }

    @Override
    @Transactional
    public void paymentFailed(UUID orderId) {
        Order order = require(orderId);
        if (order.getPaymentStatus() == PaymentStatus.FAILED) {
            return;
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT
                || (order.getPaymentStatus() != PaymentStatus.PENDING
                && order.getPaymentStatus() != PaymentStatus.PROCESSING)) {
            throw new AppException(ErrorCode.ORDER_TRANSITION_INVALID);
        }
        order.setPaymentStatus(PaymentStatus.FAILED);
    }

    @Override
    @Transactional
    public void paymentCollected(UUID orderId) {
        Order order = require(orderId);
        if (order.getPaymentStatus() == PaymentStatus.COLLECTED) {
            return;
        }
        if (order.getPaymentStatus() == PaymentStatus.CANCELLED
                || order.getPaymentStatus() == PaymentStatus.FAILED
                || order.getPaymentStatus() == PaymentStatus.REFUND_PENDING
                || order.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw new AppException(ErrorCode.ORDER_TRANSITION_INVALID);
        }
        order.setPaymentStatus(PaymentStatus.COLLECTED);
    }

    @Override
    @Transactional
    public void deliveryAssigned(UUID orderId) {
        OrderLifecycle.preparing(
                require(orderId)
        );
    }

    @Override
    @Transactional
    public void pickedUp(UUID orderId) {
        OrderLifecycle.delivering(
                require(orderId)
        );
    }

    @Override
    @Transactional
    public void delivered(UUID orderId) {
        OrderLifecycle.completed(
                require(orderId)
        );
    }

    @Override
    @Transactional
    public void matchingFailed(UUID orderId) {
        Order order = require(orderId);
        if (order.getStatus() == com.khanh.fooddelivery.order_service.enums.OrderStatus.CANCELLED
                || order.getStatus() == com.khanh.fooddelivery.order_service.enums.OrderStatus.REJECTED) {
            cancelPaymentForTerminalFailure(order);
            return;
        }
        if (order.getStatus() == com.khanh.fooddelivery.order_service.enums.OrderStatus.COMPLETED) {
            return;
        }
        OrderLifecycle.cancelMatching(order);
        cancelPaymentForTerminalFailure(order);
    }

    private void cancelPaymentForTerminalFailure(Order order) {
        if (order.getPaymentId() == null || order.getPaymentStatus() == null) {
            return;
        }
        try {
            var payment = switch (order.getPaymentStatus()) {
                case PAID, REFUND_PENDING -> paymentClient.refund(internalApiKey, order.getId());
                case PENDING, PROCESSING, FAILED -> paymentClient.cancel(internalApiKey, order.getId());
                case CANCELLED, REFUNDED, COLLECTED -> null;
            };
            if (payment != null && payment.success() && payment.data() != null) {
                order.setPaymentStatus(payment.data().status());
            } else if (payment != null) {
                throw new AppException(ErrorCode.PAYMENT_SERVICE_UNAVAILABLE,
                        "Payment cleanup was not accepted");
            }
        } catch (FeignException exception) {
            log.error("Terminal dispatch payment cleanup failed orderId={} status={}",
                    order.getId(), order.getPaymentStatus(), exception);
            throw new AppException(ErrorCode.PAYMENT_SERVICE_UNAVAILABLE,
                    "Payment cleanup is temporarily unavailable");
        }
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

    private void clearCartAfterPlacement(UUID branchId, long expectedCartVersion) {
        try {
            var response = cartClient.clear(
                    bearerTokenProvider.getBearerToken(),
                    branchId,
                    expectedCartVersion
            );
            if (response == null || !response.success()) {
                log.warn("Cart clear was not completed after order placement branchId={} expectedCartVersion={}",
                        branchId, expectedCartVersion);
            }
        } catch (FeignException exception) {
            log.warn("Cart clear failed after order placement branchId={} expectedCartVersion={} reasonType={}",
                    branchId, expectedCartVersion, exception.getClass().getSimpleName());
        } catch (RuntimeException exception) {
            log.warn("Cart clear could not be attempted after order placement branchId={} expectedCartVersion={} reasonType={}",
                    branchId, expectedCartVersion, exception.getClass().getSimpleName());
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
