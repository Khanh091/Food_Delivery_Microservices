package com.khanh.fooddelivery.order_service.placement;

import com.khanh.fooddelivery.order_service.dto.request.CheckoutDeliveryTargetRequest;
import com.khanh.fooddelivery.order_service.dto.request.CreateOrderRequest;
import com.khanh.fooddelivery.order_service.exception.AppException;
import com.khanh.fooddelivery.order_service.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class OrderPlacementFingerprint {

    public String of(CreateOrderRequest request) {
        if (request == null || request.branchId() == null || request.target() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        CheckoutDeliveryTargetRequest target = request.target();
        String canonical = String.join("\n",
                "branchId=" + request.branchId(),
                "cartVersion=" + request.cartVersion(),
                "targetType=" + normalize(target.type()),
                "addressId=" + value(target.addressId()),
                "temporaryLocationId=" + value(target.temporaryLocationId()),
                "paymentMethod=" + request.effectivePaymentMethod().name());
        return sha256(canonical);
    }

    public String keyHash(String key) {
        return sha256(key);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                hex.append(String.format("%02x", item));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
