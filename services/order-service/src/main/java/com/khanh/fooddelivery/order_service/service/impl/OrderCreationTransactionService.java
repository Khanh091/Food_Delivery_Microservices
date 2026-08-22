package com.khanh.fooddelivery.order_service.service.impl;

import com.khanh.fooddelivery.order_service.client.PaymentServiceClient;
import com.khanh.fooddelivery.order_service.client.dto.request.InternalCreatePaymentRequest;
import com.khanh.fooddelivery.order_service.client.dto.response.PaymentResponse;
import com.khanh.fooddelivery.order_service.dto.response.CheckoutPreviewResponse;
import com.khanh.fooddelivery.order_service.dto.response.OrderResponse;
import com.khanh.fooddelivery.order_service.entity.Order;
import com.khanh.fooddelivery.order_service.exception.AppException;
import com.khanh.fooddelivery.order_service.exception.ErrorCode;
import com.khanh.fooddelivery.order_service.enums.PaymentMethod;
import com.khanh.fooddelivery.order_service.mapper.OrderMapper;
import com.khanh.fooddelivery.order_service.repository.OrderRepository;
import com.khanh.fooddelivery.order_service.service.OrderSnapshotFactory;
import feign.FeignException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderCreationTransactionService {

    private final OrderRepository orders;
    private final OrderSnapshotFactory orderSnapshotFactory;
    private final PaymentServiceClient paymentClient;
    private final OrderMapper orderMapper;

    @Value("${app.internal-api.key:}")
    private String internalApiKey;

    @Transactional
    public OrderResponse create(
            UUID orderId,
            UUID customerId,
            CheckoutPreviewResponse preview,
            PaymentMethod paymentMethod
    ) {
        Order order = orderSnapshotFactory.create(orderId, customerId, preview, paymentMethod);
        orders.saveAndFlush(order);

        try {
            PaymentResponse payment = createPayment(order);
            applyPaymentProjection(order, payment);
        } catch (FeignException exception) {
            throw new AppException(ErrorCode.PAYMENT_SERVICE_UNAVAILABLE);
        }

        return orderMapper.toResponse(order);
    }

    private PaymentResponse createPayment(Order order) {
        var response = paymentClient.create(internalApiKey, new InternalCreatePaymentRequest(
                order.getId(), order.getCustomerId(), order.getRestaurantId(), order.getBranchId(),
                order.getPaymentMethod(), order.getItemsSubtotal(), order.getDeliveryFee(),
                order.getDiscountAmount(), order.getTotalAmount(), order.getCurrency(),
                "order:" + order.getId() + ":payment"));
        if (response == null || !response.success() || response.data() == null) {
            throw new AppException(ErrorCode.PAYMENT_SERVICE_UNAVAILABLE);
        }
        return response.data();
    }

    private void applyPaymentProjection(Order order, PaymentResponse payment) {
        order.setPaymentId(payment.id());
        order.setPaymentStatus(payment.status());
        order.setFeePolicyId(payment.feePolicyId());
        order.setFeePolicyVersion(payment.feePolicyVersion());
        order.setRestaurantCommissionAmount(payment.restaurantCommissionAmount());
        order.setRestaurantNetAmount(payment.restaurantNetAmount());
        order.setDriverCommissionAmount(payment.driverCommissionAmount());
        order.setDriverNetAmount(payment.driverNetAmount());
        order.setPlatformRevenueAmount(payment.platformRevenueAmount());
    }
}
