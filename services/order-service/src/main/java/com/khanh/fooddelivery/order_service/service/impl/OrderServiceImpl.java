package com.khanh.fooddelivery.order_service.service.impl;

import com.khanh.fooddelivery.order_service.client.CartServiceClient;
import com.khanh.fooddelivery.order_service.client.PaymentServiceClient;
import com.khanh.fooddelivery.order_service.client.dto.request.InternalCreatePaymentRequest;
import com.khanh.fooddelivery.order_service.dto.request.CheckoutPreviewRequest;
import com.khanh.fooddelivery.order_service.dto.request.CreateOrderRequest;
import com.khanh.fooddelivery.order_service.dto.request.RejectOrderRequest;
import com.khanh.fooddelivery.order_service.dto.response.CheckoutPreviewResponse;
import com.khanh.fooddelivery.order_service.dto.response.OrderResponse;
import com.khanh.fooddelivery.order_service.entity.Order;
import com.khanh.fooddelivery.order_service.exception.AppException;
import com.khanh.fooddelivery.order_service.exception.ErrorCode;
import com.khanh.fooddelivery.order_service.mapper.OrderMapper;
import com.khanh.fooddelivery.order_service.outbox.OrderOutboxService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orders;
    private final CheckoutPreviewService checkoutPreviewService;
    private final CurrentUserProvider currentUserProvider;
    private final CurrentBearerTokenProvider bearerTokenProvider;
    private final CartServiceClient cartClient;
    private final OrderMapper orderMapper;
    private final OrderSnapshotFactory orderSnapshotFactory;
    private final RestaurantAuthorizationService restaurantAuthorization;
    private final PaymentServiceClient paymentClient;
    private final OrderOutboxService orderOutbox;

    @Value("${app.internal-api.key:}")
    private String internalApiKey;

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
                        preview,
                        request.effectivePaymentMethod()
                );

        orders.saveAndFlush(order);

        try {
            var paymentResponse = paymentClient.create(internalApiKey, new InternalCreatePaymentRequest(
                    order.getId(), order.getCustomerId(), order.getRestaurantId(), order.getBranchId(),
                    order.getPaymentMethod(), order.getItemsSubtotal(), order.getDeliveryFee(),
                    order.getDiscountAmount(), order.getTotalAmount(), order.getCurrency(),
                    "order:" + order.getId() + ":payment"));
            if (paymentResponse == null || !paymentResponse.success() || paymentResponse.data() == null) {
                throw new AppException(ErrorCode.PAYMENT_SERVICE_UNAVAILABLE);
            }
            order.setPaymentId(paymentResponse.data().id());
            order.setPaymentStatus(paymentResponse.data().status());
            order.setFeePolicyId(paymentResponse.data().feePolicyId());
            order.setFeePolicyVersion(paymentResponse.data().feePolicyVersion());
            order.setRestaurantCommissionAmount(paymentResponse.data().restaurantCommissionAmount());
            order.setRestaurantNetAmount(paymentResponse.data().restaurantNetAmount());
            order.setDriverCommissionAmount(paymentResponse.data().driverCommissionAmount());
            order.setDriverNetAmount(paymentResponse.data().driverNetAmount());
            order.setPlatformRevenueAmount(paymentResponse.data().platformRevenueAmount());
            orders.save(order);
        } catch (FeignException exception) {
            throw new AppException(ErrorCode.PAYMENT_SERVICE_UNAVAILABLE);
        }

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
                .filter(order -> order.getStatus() != com.khanh.fooddelivery.order_service.enums.OrderStatus.PENDING_PAYMENT)
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
        orderOutbox.publishOrderConfirmed(order);
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
    public void paymentPaid(UUID orderId) {
        Order order = require(orderId);
        order.setPaymentStatus(com.khanh.fooddelivery.order_service.enums.PaymentStatus.PAID);
        OrderLifecycle.paymentPaid(order);
    }

    @Override
    public void paymentFailed(UUID orderId) {
        Order order = require(orderId);
        order.setPaymentStatus(com.khanh.fooddelivery.order_service.enums.PaymentStatus.FAILED);
    }

    @Override
    public void paymentCollected(UUID orderId) {
        Order order = require(orderId);
        order.setPaymentStatus(com.khanh.fooddelivery.order_service.enums.PaymentStatus.COLLECTED);
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
