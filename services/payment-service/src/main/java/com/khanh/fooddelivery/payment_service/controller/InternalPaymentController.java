package com.khanh.fooddelivery.payment_service.controller;

import com.khanh.fooddelivery.payment_service.common.response.ApiResponse;
import com.khanh.fooddelivery.payment_service.dto.request.CashActionRequest;
import com.khanh.fooddelivery.payment_service.dto.request.InternalCreatePaymentRequest;
import com.khanh.fooddelivery.payment_service.dto.response.FinancialFactsResponse;
import com.khanh.fooddelivery.payment_service.dto.response.PaymentResponse;
import com.khanh.fooddelivery.payment_service.security.InternalRequestAuthenticator;
import com.khanh.fooddelivery.payment_service.service.CodPaymentService;
import com.khanh.fooddelivery.payment_service.service.FinancialService;
import com.khanh.fooddelivery.payment_service.service.PaymentService;
import com.khanh.fooddelivery.payment_service.exception.PaymentException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/payments")
@RequiredArgsConstructor
public class InternalPaymentController {
    private final PaymentService payments;
    private final FinancialService financials;
    private final CodPaymentService codPayments;
    private final InternalRequestAuthenticator authenticator;

    @PostMapping
    public ApiResponse<PaymentResponse> create(@RequestHeader("X-Internal-Api-Key") String key,
                                               @RequestBody InternalCreatePaymentRequest request) {
        authenticator.authenticate(key);
        return ApiResponse.success("Payment created", payments.create(request));
    }

    @GetMapping("/orders/{orderId}/financial-facts")
    public ApiResponse<FinancialFactsResponse> facts(@RequestHeader("X-Internal-Api-Key") String key,
                                                     @PathVariable UUID orderId) {
        authenticator.authenticate(key);
        return ApiResponse.success("Financial facts", financials.facts(orderId));
    }

    @PostMapping("/deliveries/{deliveryId}/restaurant-advance-confirmed")
    public ApiResponse<Void> advance(@RequestHeader("X-Internal-Api-Key") String key,
                                     @PathVariable UUID deliveryId,
                                     @RequestBody CashActionRequest request) {
        authenticator.authenticate(key);
        ensureDeliveryPath(deliveryId, request);
        codPayments.confirmRestaurantAdvance(request);
        return ApiResponse.success("Restaurant advance confirmed", null);
    }

    @PostMapping("/deliveries/{deliveryId}/cash-collected")
    public ApiResponse<Void> collected(@RequestHeader("X-Internal-Api-Key") String key,
                                       @PathVariable UUID deliveryId,
                                       @RequestBody CashActionRequest request) {
        authenticator.authenticate(key);
        ensureDeliveryPath(deliveryId, request);
        codPayments.collectCash(request);
        return ApiResponse.success("Customer cash collected", null);
    }

    @PostMapping("/orders/{orderId}/delivery-completed")
    public ApiResponse<Void> completed(@RequestHeader("X-Internal-Api-Key") String key,
                                       @PathVariable UUID orderId,
                                       @RequestBody CashActionRequest request) {
        authenticator.authenticate(key);
        if (request == null || !orderId.equals(request.orderId())) {
            throw new PaymentException("PAYMENT_400", org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Order path and request order do not match");
        }
        financials.completeDelivery(orderId, request.deliveryId(), request.driverId());
        return ApiResponse.success("Financial positions finalized", null);
    }

    @PostMapping("/orders/{orderId}/refund")
    public ApiResponse<PaymentResponse> refund(@RequestHeader("X-Internal-Api-Key") String key,
                                               @PathVariable UUID orderId) {
        authenticator.authenticate(key);
        return ApiResponse.success("Refund requested", payments.refund(orderId));
    }

    @PostMapping("/orders/{orderId}/cancel")
    public ApiResponse<PaymentResponse> cancel(@RequestHeader("X-Internal-Api-Key") String key,
                                               @PathVariable UUID orderId) {
        authenticator.authenticate(key);
        return ApiResponse.success("Payment cancelled", payments.cancel(orderId));
    }

    private void ensureDeliveryPath(UUID deliveryId, CashActionRequest request) {
        if (request == null || request.deliveryId() == null || !deliveryId.equals(request.deliveryId())) {
            throw new PaymentException("PAYMENT_400", org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Delivery path and request delivery do not match");
        }
    }
}
