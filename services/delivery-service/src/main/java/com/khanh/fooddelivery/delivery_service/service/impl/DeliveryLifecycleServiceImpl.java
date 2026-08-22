package com.khanh.fooddelivery.delivery_service.service.impl;

import com.khanh.fooddelivery.delivery_service.client.DriverServiceClient;
import com.khanh.fooddelivery.delivery_service.client.OrderServiceClient;
import com.khanh.fooddelivery.delivery_service.client.PaymentServiceClient;
import com.khanh.fooddelivery.delivery_service.client.dto.request.CashActionRequest;
import com.khanh.fooddelivery.delivery_service.dto.response.DeliveryOfferResponse;
import com.khanh.fooddelivery.delivery_service.dto.response.CurrentDeliveryOfferResponse;
import com.khanh.fooddelivery.delivery_service.dto.response.DeliveryResponse;
import com.khanh.fooddelivery.delivery_service.exception.AppException;
import com.khanh.fooddelivery.delivery_service.exception.ErrorCode;
import com.khanh.fooddelivery.delivery_service.mapper.DeliveryMapper;
import com.khanh.fooddelivery.delivery_service.model.Delivery;
import com.khanh.fooddelivery.delivery_service.model.DeliveryStatus;
import com.khanh.fooddelivery.delivery_service.repository.DeliveryRepository;
import com.khanh.fooddelivery.delivery_service.security.CurrentBearerTokenProvider;
import com.khanh.fooddelivery.delivery_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.delivery_service.service.DeliveryLifecycleService;
import com.khanh.fooddelivery.delivery_service.service.DeliveryOfferService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryLifecycleServiceImpl implements DeliveryLifecycleService {

    private final DeliveryRepository deliveries;
    private final CurrentUserProvider users;
    private final CurrentBearerTokenProvider bearer;
    private final OrderServiceClient orders;
    private final DriverServiceClient drivers;
    private final DeliveryOfferService offerService;
    private final DeliveryMapper mapper;
    private final PaymentServiceClient payments;

    @Value("${app.internal-api.key:}")
    private String internalApiKey;

    @Override
    public List<DeliveryOfferResponse> offers(Jwt jwt) {
        return offerService.offers(jwt);
    }

    @Override
    public Optional<CurrentDeliveryOfferResponse> currentOffer(Jwt jwt) {
        return offerService.currentOffer(jwt);
    }

    @Override
    public Optional<DeliveryResponse> currentActiveDelivery(Jwt jwt) {
        return offerService.currentActiveDelivery(jwt);
    }

    @Override
    public DeliveryResponse accept(Jwt jwt, UUID deliveryId) {
        return offerService.accept(jwt, deliveryId);
    }

    @Override
    public void reject(Jwt jwt, UUID deliveryId) {
        offerService.reject(jwt, deliveryId);
    }

    @Override
    public DeliveryResponse pickup(Jwt jwt, UUID deliveryId) {
        Delivery delivery = mine(jwt, deliveryId);
        if (delivery.getStatus() != DeliveryStatus.ASSIGNED) {
            throw new AppException(
                    ErrorCode.DELIVERY_CONFLICT,
                    "Delivery cannot be picked up"
            );
        }
        if ("COD".equals(delivery.getPaymentMethod()) && !delivery.isRestaurantAdvanceConfirmed()) {
            throw new AppException(ErrorCode.DELIVERY_CONFLICT, "Restaurant advance must be confirmed first");
        }
        delivery.setStatus(DeliveryStatus.PICKED_UP);
        orders.pickedUp(bearer.getBearerToken(), delivery.getOrderId());
        return mapper.toResponse(delivery);
    }

    @Override
    public DeliveryResponse delivered(Jwt jwt, UUID deliveryId) {
        Delivery delivery = mine(jwt, deliveryId);
        if (delivery.getStatus() != DeliveryStatus.PICKED_UP) {
            throw new AppException(
                    ErrorCode.DELIVERY_CONFLICT,
                    "Delivery cannot be completed"
            );
        }
        if ("COD".equals(delivery.getPaymentMethod()) && !delivery.isCustomerCashCollected()) {
            throw new AppException(ErrorCode.DELIVERY_CONFLICT, "Customer cash must be collected first");
        }
        if (delivery.getPaymentMethod() != null) {
            payments.deliveryCompleted(paymentCredential(), delivery.getOrderId(),
                    new CashActionRequest(delivery.getOrderId(), deliveryId, delivery.getDriverId(),
                            "delivery:" + deliveryId + ":completed"));
        }
        delivery.setStatus(DeliveryStatus.DELIVERED);
        orders.delivered(bearer.getBearerToken(), delivery.getOrderId());
        drivers.release(bearer.getBearerToken(), delivery.getDriverId(), deliveryId);
        return mapper.toResponse(delivery);
    }

    @Override
    public DeliveryResponse confirmRestaurantPayment(Jwt jwt, UUID deliveryId) {
        Delivery delivery = mine(jwt, deliveryId);
        if (delivery.getStatus() != DeliveryStatus.ASSIGNED || !"COD".equals(delivery.getPaymentMethod())) {
            throw new AppException(ErrorCode.DELIVERY_CONFLICT, "Restaurant cash advance is not available");
        }
        payments.restaurantAdvance(paymentCredential(), deliveryId,
                new CashActionRequest(delivery.getOrderId(), deliveryId, delivery.getDriverId(),
                        "delivery:" + deliveryId + ":advance"));
        delivery.setRestaurantAdvanceConfirmed(true);
        return mapper.toResponse(delivery);
    }

    @Override
    public DeliveryResponse collectCash(Jwt jwt, UUID deliveryId) {
        Delivery delivery = mine(jwt, deliveryId);
        if (delivery.getStatus() != DeliveryStatus.PICKED_UP || !"COD".equals(delivery.getPaymentMethod())) {
            throw new AppException(ErrorCode.DELIVERY_CONFLICT, "Customer cash collection is not available");
        }
        payments.cashCollected(paymentCredential(), deliveryId,
                new CashActionRequest(delivery.getOrderId(), deliveryId, delivery.getDriverId(),
                        "delivery:" + deliveryId + ":cash-collected"));
        delivery.setCustomerCashCollected(true);
        return mapper.toResponse(delivery);
    }

    @Override
    public void expireOffers() {
        offerService.expireOffers();
    }

    private Delivery mine(Jwt jwt, UUID deliveryId) {
        Delivery delivery = deliveries.findByIdForUpdate(deliveryId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.DELIVERY_CONFLICT,
                        "Delivery is no longer available"
                ));
        if (!users.getCurrentUserId(jwt).equals(delivery.getDriverId())) {
            throw new AppException(
                    ErrorCode.ACCESS_DENIED,
                    "Only the assigned driver can update this delivery"
            );
        }
        return delivery;
    }

    private String downstreamCredential() {
        try {
            return bearer.getBearerToken();
        } catch (AppException exception) {
            if (internalApiKey == null || internalApiKey.isBlank()) throw exception;
            return "Internal " + internalApiKey;
        }
    }

    private String paymentCredential() {
        if (internalApiKey == null || internalApiKey.isBlank()) {
            throw new AppException(ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE,
                    "Payment service internal credential is not configured");
        }
        return internalApiKey;
    }
}
